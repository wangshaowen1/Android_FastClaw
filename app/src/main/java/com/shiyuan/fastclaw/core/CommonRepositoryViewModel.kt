package com.shiyuan.fastclaw.core

import android.net.Uri
import android.util.Log
import com.alibaba.fastjson.JSON
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.shiyuan.fastclaw.BR
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.PollingState
import com.shiyuan.fastclaw.core.ext.launchPolling
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.ContentBody
import com.shiyuan.fastclaw.core.network.apis.request.CompleteFileUploadBody
import com.shiyuan.fastclaw.core.network.apis.request.CompleteFileUploadItemBody
import com.shiyuan.fastclaw.core.network.apis.request.CreateFileUploadCredentialBody
import com.shiyuan.fastclaw.core.network.apis.request.FileSecurityStatusQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.FileUploadCredentialItemBody
import com.shiyuan.fastclaw.core.network.apis.response.FileSecurityStatusData
import com.shiyuan.fastclaw.core.network.apis.response.FileUploadCredentialData
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.viewmodel.BaseRepositoryViewModel
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.common.ext.cleanupTempFile
import com.shiyuan.fastclaw.common.ext.isNetworkPath
import com.shiyuan.fastclaw.common.ext.toOssFilePath
import kotlinx.coroutines.Job
import java.io.File
import java.util.Locale
import kotlin.collections.forEach
import kotlin.collections.get

open class CommonRepositoryViewModel<T : AppRepository> : AppRepositoryViewModel<T>() {
    private var fileSecurityPollingJob: Job? = null
    private var isSecureFileUploadActive = false

    // 可选：定义上传结果数据类
    data class UploadResult(val successUrls: List<String>, val failedUris: List<Uri>)

    /**
     * 单文件上传（保持原有逻辑，内部调用多文件版本）
     */
    fun submitResume(uri: Uri, callback: (path: String) -> Unit) {
        submitMultipleFiles(listOf(uri)) { successUrls, failedUris ->
            if (successUrls.isNotEmpty()) {
                callback(successUrls[0])
            } else {
                // 上传失败处理
                "文件上传失败".showToast()
            }
        }
    }

    /**
     * 多文件上传
     * @param uris 待上传的文件 Uri 列表
     * @param onComplete 所有文件上传完成后的回调，返回成功上传的 URL 列表和失败的 Uri 列表
     * 当前 OSS 阿里云批量上传最终回调是二选一：
     */
    fun submitMultipleFiles(
        uris: List<Uri>,
        onComplete: (successUrls: List<String>, failedUris: List<Uri>) -> Unit
    ) {
        if (uris.isEmpty()) {
            onComplete(emptyList(), emptyList())
            return
        }

        // setLoading(true)
        val context = App.instance

        // 先完成路径转换，再统一走 OSS 多文件上传。
        val failedUris = mutableListOf<Uri>()
        val uploadFiles = uris.mapNotNull { uri ->
            uri.toOssFilePath(context)?.let { filePath ->
                uri to filePath
            } ?: run {
                failedUris.add(uri)
                null
            }
        }

        if (uploadFiles.isEmpty()) {
            handleUploadComplete(emptyList(), failedUris, onComplete)
            return
        }

        val ossClient = OssClient.newInstance(context)
        ossClient.uploadFile(
            uploadFiles.map { it.second },
            OssClient.TYPE_NORMAL,
            object : OnUploadListener() {
                override fun onUploadSuccess(uploadSuccess: List<String?>) {
                    val successUrls = mutableListOf<String>()
                    uploadFiles.forEachIndexed { index, (uri, filePath) ->
                        val url = uploadSuccess.getOrNull(index)
                        if (url == null) {
                            failedUris.add(uri)
                        } else {
                            successUrls.add(url)
                        }
                        if (!uri.isNetworkPath()) {
                            filePath.cleanupTempFile()
                        }
                    }
                    handleUploadComplete(successUrls, failedUris, onComplete)
                }

                override fun onUploadFail(uploadFail: MutableList<String>?) {
                    val failedPaths = uploadFail.orEmpty().toSet()
                    if (failedPaths.isNotEmpty()) {
                        uploadFiles.filter { it.second !in failedPaths }.forEach { (uri, filePath) ->
                            if (!uri.isNetworkPath()) {
                                filePath.cleanupTempFile()
                            }
                        }
                    }
                    val uploadFailedUris = if (failedPaths.isEmpty()) {
                        uploadFiles.map { it.first }
                    } else {
                        uploadFiles.filter { it.second in failedPaths }.map { it.first }
                    }
                    failedUris.addAll(uploadFailedUris)
                    // 批量接口失败回调仅提供失败路径，无法返回其余文件的成功 URL。
                    handleUploadComplete(emptyList(), failedUris.distinct(), onComplete)
                }
            }
        )
    }

    /**
     * 处理所有上传完成后的逻辑
     */
    private fun handleUploadComplete(
        successUrls: List<String>,
        failedUris: List<Uri>,
        onComplete: (List<String>, List<Uri>) -> Unit
    ) {
        // setLoading(false)
        if (failedUris.isNotEmpty()) {
            // 可以根据需要提示失败数量
            "有 ${failedUris.size} 个文件上传失败".showToast()
        }
        onComplete(successUrls, failedUris)
    }

    /**
     * 创建凭证、上传 OSS、确认上传完成并轮询文件安全状态。
     *
     * 四个步骤严格串行，任一步失败都会通过 [onFailure] 返回原因。
     * [onSuccess] 返回安全审核通过后的 OSS 上传成功 URL 列表。
     * @param uriList 待上传文件 Uri 列表
     * @param bizType 文件上传业务类型，使用 [FileUploadCredentialItemBody] 中定义的常量
     */
    fun uploadFilesWithSecurityCheck(
        uriList: List<Uri>,
        bizType: String,
        onSuccess: (successUrls: List<String>) -> Unit,
        onFailure: (reason: String) -> Unit,
    ) {
        if (isSecureFileUploadActive) {
            onFailure("已有文件上传任务正在进行中")
            return
        }

        val uploadFiles = buildSecureUploadFiles(uriList, bizType, onFailure) ?: return
        isSecureFileUploadActive = true

        requestWithLoading(
            apiBlock = {
                mRepo.api.createFileUploadCredential(
                    CreateFileUploadCredentialBody(
                        files = uploadFiles.map { it.request },
                    )
                )
            },
            showLoading = false,
            showToastError = false,
            onError = { throwable ->
                finishSecureFileUploadFailure(
                    throwable.reasonOr("创建上传凭证失败"),
                    onFailure,
                )
            },
        ) {
            val credentials = data.orEmpty()
            val credentialError = validateCredentials(uploadFiles, credentials)
            if (credentialError != null) {
                finishSecureFileUploadFailure(credentialError, onFailure)
                return@requestWithLoading
            }
            uploadFilesToOss(
                uploadFiles,
                orderCredentialsByUploadFiles(uploadFiles, credentials),
                onSuccess,
                onFailure,
            )
        }
    }

    private fun buildSecureUploadFiles(
        uriList: List<Uri>,
        bizType: String,
        onFailure: (String) -> Unit,
    ): List<SecureUploadFile>? {
        if (uriList.isEmpty()) {
            onFailure("待上传文件不能为空")
            return null
        }
        if (uriList.distinct().size != uriList.size) {
            onFailure("待上传文件不能重复")
            return null
        }

        val uploadFiles = mutableListOf<SecureUploadFile>()
        val uploadPaths = mutableSetOf<String>()
        uriList.forEach { uri ->
            val path = uri.toOssFilePath(App.instance)
            if (path.isNullOrBlank()) {
                onFailure("文件 Uri 无法转换为本地路径")
                return null
            }
            if (!uploadPaths.add(path)) {
                onFailure("待上传文件路径不能重复")
                return null
            }
            val file = File(path)
            if (!file.isFile || !file.canRead()) {
                onFailure("文件不存在或无法读取：${file.name.ifBlank { path }}")
                return null
            }
            val fileType = file.extension.lowercase(Locale.ROOT)
            if (fileType.isBlank()) {
                onFailure("无法识别文件类型：${file.name}")
                return null
            }
            uploadFiles += SecureUploadFile(
                path = path,
                request = FileUploadCredentialItemBody(
                    bizType = bizType,
                    fileName = file.name,
                    fileType = fileType,
                    fileSize = file.length(),
                ),
            )
        }
        return uploadFiles
    }

    private fun validateCredentials(
        uploadFiles: List<SecureUploadFile>,
        credentials: List<FileUploadCredentialData>,
    ): String? {
        if (credentials.size != uploadFiles.size) {
            return "上传凭证数量与文件数量不一致"
        }
        credentials.forEachIndexed { index, credential ->
            if (credential.fileRecordId.isNullOrBlank()) {
                return "第 ${index + 1} 个文件缺少 fileRecordId"
            }
            if (credential.objectKey.isNullOrBlank()) {
                return "第 ${index + 1} 个文件缺少 objectKey"
            }
            if (credential.fileName.isNullOrBlank()) {
                return "第 ${index + 1} 个文件缺少 fileName"
            }
            if (credential.fileType.isNullOrBlank()) {
                return "第 ${index + 1} 个文件缺少 fileType"
            }
            if (credential.fileSize == null) {
                return "第 ${index + 1} 个文件缺少 fileSize"
            }
        }
        if (credentials.map { it.bucket?.takeIf(String::isNotBlank) }.distinct().size != 1) {
            return "批量上传凭证的 bucket 不一致"
        }
        val requestFileCounts = uploadFiles.groupingBy {
            Triple(it.request.fileName, it.request.fileType, it.request.fileSize)
        }.eachCount()
        val credentialFileCounts = credentials.groupingBy {
            Triple(it.fileName.orEmpty(), it.fileType.orEmpty(), it.fileSize)
        }.eachCount()
        if (requestFileCounts != credentialFileCounts) {
            return "上传凭证与本地文件无法匹配"
        }
        return null
    }

    private fun orderCredentialsByUploadFiles(
        uploadFiles: List<SecureUploadFile>,
        credentials: List<FileUploadCredentialData>,
    ): List<FileUploadCredentialData> {
        val remainingCredentials = credentials.toMutableList()
        return uploadFiles.map { uploadFile ->
            val credentialIndex = remainingCredentials.indexOfFirst {
                it.fileName == uploadFile.request.fileName &&
                    it.fileType == uploadFile.request.fileType &&
                    it.fileSize == uploadFile.request.fileSize
            }
            remainingCredentials.removeAt(credentialIndex)
        }
    }

    private fun uploadFilesToOss(
        uploadFiles: List<SecureUploadFile>,
        credentials: List<FileUploadCredentialData>,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val pathToObjectKey = LinkedHashMap<String, String>(uploadFiles.size)
        uploadFiles.forEachIndexed { index, uploadFile ->
            pathToObjectKey[uploadFile.path] = requireNotNull(credentials[index].objectKey)
        }

        val listener = object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                if (!isSecureFileUploadActive) return
                if (uploadSuccess.size != uploadFiles.size || uploadSuccess.any { it.isNullOrBlank() }) {
                    finishSecureFileUploadFailure("OSS 上传结果数量与文件数量不一致", onFailure)
                    return
                }
                Log.d("OnUploadListener", JSON.toJSONString(uploadSuccess))
                confirmFileUpload(
                    credentials,
                    uploadSuccess.map { requireNotNull(it) },
                    onSuccess,
                    onFailure,
                )
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                if (!isSecureFileUploadActive) return
                val failedFiles = uploadFail.orEmpty()
                    .map { File(it).name.ifBlank { it } }
                    .distinct()
                val reason = if (failedFiles.isEmpty()) {
                    "OSS 文件上传失败"
                } else {
                    "OSS 文件上传失败：${failedFiles.joinToString()}"
                }
                finishSecureFileUploadFailure(reason, onFailure)
            }
        }

        val ossClient = OssClient.newInstance(App.instance)
        try {
            ossClient.uploadFile(
                credentials.first().bucket?.takeIf(String::isNotBlank),
                pathToObjectKey,
                OssClient.TYPE_NORMAL,
                listener,
            )
        } catch (throwable: Throwable) {
            finishSecureFileUploadFailure(throwable.reasonOr("OSS 文件上传失败"), onFailure)
        }
    }

    private fun confirmFileUpload(
        credentials: List<FileUploadCredentialData>,
        successUrls: List<String>,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val fileRecordIds = credentials.map { requireNotNull(it.fileRecordId) }
        requestWithLoading(
            apiBlock = {
                mRepo.api.completeFileUpload(
                    CompleteFileUploadBody(
                        files = fileRecordIds.map { CompleteFileUploadItemBody(fileRecordId = it) }
                    )
                )
            },
            showLoading = false,
            showToastError = false,
            onError = { throwable ->
                finishSecureFileUploadFailure(
                    throwable.reasonOr("确认文件上传完成失败"),
                    onFailure,
                )
            },
        ) {
            val confirmedIds = data.orEmpty().mapNotNull { it.fileRecordId }.toSet()
            if (!confirmedIds.containsAll(fileRecordIds)) {
                finishSecureFileUploadFailure("确认上传完成结果不完整", onFailure)
                return@requestWithLoading
            }
            pollFileSecurityStatus(fileRecordIds, successUrls, onSuccess, onFailure)
        }
    }

    private fun pollFileSecurityStatus(
        fileRecordIds: List<String>,
        successUrls: List<String>,
        onSuccess: (List<String>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        var failureReason: String? = null
        fileSecurityPollingJob?.cancel()
        fileSecurityPollingJob = launchPolling(
            intervalMs = FILE_SECURITY_POLL_INTERVAL_MS,
            retryOnNetworkError = false,
            showToastError = false,
            apiBlock = {
                mRepo.api.queryFileSecurityStatus(
                    FileSecurityStatusQueryBody(fileRecordIds = fileRecordIds)
                )
            },
            checkState = { response ->
                when {
                    response?.isSuccess != true -> {
                        failureReason = response?.msg ?: "查询文件安全状态失败"
                        PollingState.FAIL
                    }
                    response?.data.orEmpty().mapNotNull { it.fileRecordId }.toSet()
                        .containsAll(fileRecordIds).not() -> {
                        failureReason = "文件安全状态返回不完整"
                        PollingState.FAIL
                    }
                    response?.data.orEmpty().any { it.securityStatus in FILE_SECURITY_FAILED_STATUSES } -> {
                        failureReason = buildSecurityFailureReason(response?.data.orEmpty())
                        PollingState.FAIL
                    }
                    response?.data.orEmpty().all { it.securityStatus == FILE_SECURITY_STATUS_PASSED } -> {
                        PollingState.SUCCESS
                    }
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = {
                fileSecurityPollingJob = null
                if (!isSecureFileUploadActive) return@launchPolling
                isSecureFileUploadActive = false
                onSuccess(successUrls)
            },
            onFail = { response, throwable ->
                fileSecurityPollingJob = null
                finishSecureFileUploadFailure(
                    failureReason
                        ?: response?.msg
                        ?: throwable.reasonOr("查询文件安全状态失败"),
                    onFailure,
                )
            },
        )
    }

    private fun buildSecurityFailureReason(statusList: List<FileSecurityStatusData>): String {
        val reasons = statusList
            .filter { it.securityStatus in FILE_SECURITY_FAILED_STATUSES }
            .map { status ->
                status.failReason?.takeIf { it.isNotBlank() }
                    ?: "文件 ${status.fileRecordId.orEmpty()} 安全审核失败"
            }
            .distinct()
        return reasons.joinToString("；").ifBlank { "文件安全审核失败" }
    }

    private fun finishSecureFileUploadFailure(
        reason: String,
        onFailure: (String) -> Unit,
    ) {
        if (!isSecureFileUploadActive) return
        fileSecurityPollingJob?.cancel()
        fileSecurityPollingJob = null
        isSecureFileUploadActive = false
        onFailure(reason)
    }

    private fun Throwable?.reasonOr(defaultReason: String): String {
        return this?.message?.takeIf { it.isNotBlank() } ?: defaultReason
    }

    override fun onCleared() {
        isSecureFileUploadActive = false
        fileSecurityPollingJob?.cancel()
        fileSecurityPollingJob = null
        super.onCleared()
    }

    private data class SecureUploadFile(
        val path: String,
        val request: FileUploadCredentialItemBody,
    )

    private companion object {
        const val FILE_SECURITY_POLL_INTERVAL_MS = 3000L
        const val FILE_SECURITY_STATUS_PASSED = 30
        val FILE_SECURITY_FAILED_STATUSES = setOf(40, 50)
    }
}
