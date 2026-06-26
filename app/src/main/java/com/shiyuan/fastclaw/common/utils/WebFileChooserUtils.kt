package com.shiyuan.fastclaw.common.utils

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.WebChromeClient

object WebFileChooserUtils {

    fun buildFileChooserIntent(fileChooserParams: WebChromeClient.FileChooserParams?): Intent {
        val acceptTypes = fileChooserParams?.acceptTypes.orEmpty()
            .flatMap { it.split(",") }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        val chooseAll = acceptTypes.isEmpty() ||
                acceptTypes.any { it == "all" || it == ".all" || it == "*" || it == "*/*" }
        val mimeTypes = if (chooseAll) {
            emptyList()
        } else {
            acceptTypes.mapNotNull { it.toChooserMimeType() }.distinct()
        }

        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeTypes.singleOrNull() ?: "*/*"
            if (mimeTypes.size > 1) {
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
            }
            putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
            )
        }
    }

    fun parseFileChooserResult(resultCode: Int, data: Intent?): Array<Uri?>? {
        return WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            ?.map { it as Uri? }
            ?.toTypedArray()
    }

    private fun String.toChooserMimeType(): String? {
        if (contains("/")) return this
        val extension = removePrefix(".")
        return when (extension) {
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "rar" -> "application/vnd.rar"
            "7z" -> "application/x-7z-compressed"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
    }
}
