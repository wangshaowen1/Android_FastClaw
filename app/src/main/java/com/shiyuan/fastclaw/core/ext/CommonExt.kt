@file:JvmName("CommonUtils")

package com.shiyuan.fastclaw.core.ext

import android.content.Intent
import android.net.Uri
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.core.network.apis.ApiException
import com.shiyuan.fastclaw.core.network.apis.ApiResponse
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

fun String.openWithBrowser() {
    val url = this
    if (url.isEmpty()) {
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        App.instance.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        "未找到浏览器应用".showToast()
    }
}

suspend fun ResponseBody.saveToFile(file: File): Boolean = withContext(Dispatchers.IO) {
    try {
        val inputStream = byteStream()
        val outputStream = FileOutputStream(file)

        val buffer = ByteArray(4096)
        var bytes: Int
        while (inputStream.read(buffer).also { bytes = it } != -1) {
            outputStream.write(buffer, 0, bytes)
        }

        outputStream.flush()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


fun ApiResponse<*>.exception() = ApiException(code, msg)

/**
 * Bundle获取传递的值
 *
 * @param key
 * @param defaultValue
 */
fun <Value> Intent?.getBundleValue(key: String?, defaultValue: Value): Value {
    val result: Any? = this?.extras?.get(key)
    if (result != null) {
        try {
            return result as Value
        } catch (ignore: ClassCastException) {
        }
    }
    return defaultValue
}

/**
 * Bundle获取传递的值
 *
 * @param key
 */
fun <Value> Intent?.getBundleValue(key: String?): Value? {
    val result: Any? = this?.extras?.get(key)
    if (result != null) {
        try {
            return result as Value
        } catch (ignore: ClassCastException) {
        }
    }
    return null
}


