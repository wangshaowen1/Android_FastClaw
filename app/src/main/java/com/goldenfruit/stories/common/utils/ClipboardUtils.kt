package com.kgmedia.fastfilm.common.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.nhsd.basic.ext.showToast

/**
 * 剪贴板工具
 * 原 Utils.copyToClipboard 迁移至此
 */
object ClipboardUtils {

    fun copyText(context: Context?, content: String?, toast: String? = "已复制到剪贴板") {
        if (content.isNullOrEmpty()) return
        try {
            val cm = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val mClipData = ClipData.newPlainText("Label", content)
            cm?.setPrimaryClip(mClipData)
            toast?.showToast()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}