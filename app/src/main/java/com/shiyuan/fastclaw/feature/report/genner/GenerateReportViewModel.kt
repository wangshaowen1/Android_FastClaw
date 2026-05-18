package com.shiyuan.fastclaw.feature.report.genner

import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.common.Constants.Extra.IMAGE
import com.shiyuan.fastclaw.common.ext.cleanupTempFile
import com.shiyuan.fastclaw.common.ext.isNetworkPath
import com.shiyuan.fastclaw.common.ext.toOssFilePath
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.setBlackDialog
import com.shiyuan.fastclaw.core.network.apis.entity.ReportData
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.InterviewReportData
import com.shiyuan.fastclaw.core.network.apis.request.JdMaterialQualityBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeMaterialQualityBody
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.FAILURE
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.PENDING

/**
 * @Introduce: 历史报告列表 ViewModel
 */
class GenerateReportViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.generate_report.getResString(),
        background = Color.TRANSPARENT,
    )

    val mToolbarVM2 = ToolbarViewModel(
        navigationAction = { finish() },
        title = "",
        background = Color.TRANSPARENT,
    )
    val resumeData = MutableLiveData<ResumeData>()

    val isJd = MutableLiveData<Boolean>()

    val jdTextData = MutableLiveData<ResumeData>()
    val jdImageData = MutableLiveData<ResumeData>()
    val reportData = MutableLiveData<ReportData>()


    val photoSize = MutableLiveData<Int>(0)

    val tableSelect = MutableLiveData<Int>(0)


    fun interviewReport(data: InterviewReportData, callback: () -> Unit) {
        launchWithLoading(true, block = {
            response(
                mRepo.api.interviewReport(
                    data
                ), result = {
                    reportData.postValue(this.data)
                }, error = {
                    Log.d("interviewReport", this?.msg.toString())
                    setLoading(false)
                    callback.invoke()
                    this?.msg?.showToast()
                }
            )
        }, onError = {
            setLoading(false)
            it.message.toString().showToast()
        })
    }


    fun interviewReportr() {
        launchWithLoading(true, block = {
            response(
                mRepo.api.interviewReportResult(
                    InterviewReportData(
                        reportId = reportData.value?.reportId
                    )
                ), result = {
                    reportData.postValue(this.data)
                }, error = {
                    setLoading(false)
                    this?.msg?.showToast()
                }
            )
        }, onError = {
            setLoading(false)
            it.message.toString().showToast()
        })
    }

    fun jdQualityQuality(jdImageUrls: List<String>?, jdText: String?, type: String) {
        setLoading(true)
        launch(block = {
            response(
                mRepo.api.jdMaterialQuality(
                    JdMaterialQualityBody(
                        jdInputType = type,
                        jdText = jdText,
                        jdImageUrls = jdImageUrls
                    )
                ), result = {
                    if (type == IMAGE) {
                        jdImageData.postValue(this.data)
                    } else {
                        jdTextData.postValue(this.data)
                    }
                }, error = {
                    setLoading(false)
                    this?.msg?.showToast()
                }
            )
        }, onError = {
            setLoading(false)
            it.message.toString().showToast()
        })

    }

    fun jdMaterialQualityr(type: String) {
        launch(block = {
            val body = if (type == IMAGE) {
                JdMaterialQualityBody(checkId = jdImageData.value?.checkId)
            } else {
                JdMaterialQualityBody(checkId = jdTextData.value?.checkId)
            }
            response(
                mRepo.api.jdMaterialQualityResult(
                    body
                ), result = {
                    if (type == IMAGE) {
                        jdImageData.postValue(this.data)
                    } else {
                        jdTextData.postValue(this.data)
                    }
                }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

    fun resumeMaterialQuality(resumeFileUrl: String, resumeFileName: String) {
         launch(block = {
            response(
                mRepo.api.resumeMaterialQuality(
                    ResumeMaterialQualityBody(
                        resumeFileUrl = resumeFileUrl,
                        resumeFileName = resumeFileName
                    )
                ), result = {
                    resumeData.postValue(this.data)
                }, error = {
                    val errorData = resumeData.value?.copy(
                        blockingIssuesMessage = "简历解析失败",
                        checkStatus = FAILURE
                    )
                    errorData?.let {
                        resumeData.postValue(it)
                    }
                    this?.msg?.showToast()
                })
        }, onError = {
            val errorData = resumeData.value?.copy(
                blockingIssuesMessage = "简历解析失败",
                checkStatus = FAILURE
            )
            errorData?.let { it ->
                resumeData.postValue(it)
            }
            it.message.toString().showToast()
        })
    }



    fun resumeMaterialQualityr() {
        launch(block = {
            response(
                mRepo.api.resumeMaterialQualityResult(
                    ResumeMaterialQualityBody(
                        checkId = resumeData.value?.checkId
                    )
                ), result = {
                    resumeData.postValue(this.data)
                }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })
    }


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

        // 用于收集结果
        val successUrls = mutableListOf<String>()
        val failedUris = mutableListOf<Uri>()
        val lock = Any()
        var pendingCount = uris.size

        uris.forEach { uri ->
            // 转换为本地文件路径
            val filePath = uri.toOssFilePath(context)
            if (filePath == null) {
                synchronized(lock) {
                    pendingCount--
                    failedUris.add(uri)
                    if (pendingCount == 0) {
                        handleUploadComplete(successUrls, failedUris, onComplete)
                    }
                }
                return@forEach
            }

            val ossClient = OssClient.newInstance(context)
            ossClient.uploadFile(filePath, OssClient.TYPE_NORMAL, object : OnUploadListener() {
                override fun onUploadSuccess(uploadSuccess: List<String?>) {
                    val url = uploadSuccess.firstOrNull()
                    synchronized(lock) {
                        if (url != null) {
                            successUrls.add(url)
                        } else {
                            failedUris.add(uri)
                        }
                        // 清理临时文件
                        if (!uri.isNetworkPath()) {
                            filePath.cleanupTempFile()
                        }
                        pendingCount--
                        if (pendingCount == 0) {
                            handleUploadComplete(successUrls, failedUris, onComplete)
                        }
                    }
                }

                override fun onUploadFail(uploadFail: MutableList<String>?) {
                    synchronized(lock) {
                        failedUris.add(uri)
                        pendingCount--
                        if (pendingCount == 0) {
                            handleUploadComplete(successUrls, failedUris, onComplete)
                        }
                    }
                }
            })
        }
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

    override fun onCreate() {
        super.onCreate()


    }


}
