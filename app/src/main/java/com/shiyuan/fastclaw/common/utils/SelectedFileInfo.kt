package com.shiyuan.fastclaw.feature.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * 文件选择结果数据类
 */
data class SelectedFileInfo(
    val uri: Uri,          // 文件 Uri，可直接用于上传
    val fileName: String?, // 文件名（可能为空，某些 Uri 无法解析）
    val mimeType: String?, // MIME 类型，如 application/pdf、image/png
    val fileSize: Long     // 文件大小（字节），-1 表示未知
)

/**
 * 文件选择工具类（基于 Activity Result API）
 * @param fragment 发起选择的 Fragment
 * @param maxSizeBytes 允许的最大文件大小（字节），默认 10MB = 10 * 1024 * 1024
 * @param onResult 选择结果回调，成功返回 SelectedFileInfo，失败返回 null（包括大小超限）
 */
class FileChooser(
    private val fragment: Fragment,
    private val maxSizeBytes: Long = 10 * 1024 * 1024, // 10MB
    private val canSelectImage: Boolean = true,
    private val onResult: (SelectedFileInfo?) -> Unit,
    private val onSizeExceeded: (() -> Unit)? = null
) {

    // 支持的 MIME 类型：PDF、Word（doc/docx）、图片
    private val supportedMimeTypes = mutableListOf(
        "application/pdf",
        "application/msword",                                                      // .doc
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"  // .docx
    ).apply {
        if (canSelectImage) {
            addAll(
                listOf(
                    "image/jpeg",
                    "image/png",
                    "image/gif",
                    "image/webp"
                )
            )
        }
    }.toTypedArray()

    // 注册 ActivityResultLauncher
    private val pickFileLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri = data?.data
            if (uri != null) {
                // 解析文件信息并进行大小检查
                val info = getFileInfoFromUri(fragment.requireContext(), uri)
                if (info != null && info.fileSize <= maxSizeBytes) {
                    onResult(info)
                } else {
                    // 文件不存在、解析失败或超过大小限制
                    if (info != null && info.fileSize > maxSizeBytes) {
                        val sizeMB = maxSizeBytes / (1024f * 1024f)
                        // 注意：这里不能直接使用 showToast，需要传入上下文；实际使用时由外部处理提示
                        // 我们将错误信息通过回调 null 并可选附带错误消息，简化处理：回调 null 并让调用方决定提示
                    }
                    onResult(null)
                }
            } else {
                onResult(null)
            }
        } else {
            onResult(null)
        }
    }

    /**
     * 启动文件选择器
     */
    fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickFileLauncher.launch(intent)
    }

    /**
     * 从 Uri 解析文件信息（文件名、MIME类型、大小）
     */
    private fun getFileInfoFromUri(context: Context, uri: Uri): SelectedFileInfo? {
        return try {
            val fileName = getFileName(context, uri)
            val mimeType = context.contentResolver.getType(uri)
            val fileSize = getFileSize(context, uri)
            SelectedFileInfo(uri, fileName, mimeType, fileSize)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.path?.let { path ->
                path.substringAfterLast("/")
            }
        }
        return fileName
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        return it.getLong(sizeIndex)
                    }
                }
            }
            // 如果无法获取，尝试打开流获取大小
            context.contentResolver.openInputStream(uri)?.use { stream ->
                return stream.available().toLong()
            } ?: -1
        } catch (e: Exception) {
            -1
        }
    }
}
