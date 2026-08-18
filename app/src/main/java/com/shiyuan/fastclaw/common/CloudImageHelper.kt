package com.shiyuan.fastclaw.common

import android.graphics.Color
import android.util.LruCache
import com.kuaige.imageloader.AverageHueUrlStrategy
import com.kuaige.imageloader.CloudImageConfig
import com.kuaige.imageloader.ImageFormat
import com.kuaige.imageloader.ImageProcessingOptions
import com.kuaige.imageloader.Resolution
import com.nhsd.basic.utils.Logger
import com.shiyuan.fastclaw.common.ext.client
import com.shiyuan.fastclaw.common.ext.ioScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

object CloudImageHelper {
    private val colorCache by lazy { LruCache<String, Int>(500) }

    suspend fun fetchImageColor(imageUrl: String): Int? = runCatching {
        colorCache[imageUrl]?.let { return it }
        val strategy = CloudImageConfig.processor.findStrategy(imageUrl)
            as? AverageHueUrlStrategy
            ?: return null
        val colorUrl = strategy.buildAverageHueUrl(imageUrl) ?: return null
        val request = Request.Builder().url(colorUrl).build()
        val response = client.newCall(request).execute()
        response.use {
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val rgbValue = JSONObject(body).getString("RGB")
            val color = Color.parseColor("#${rgbValue.substring(2)}")
            colorCache.put(imageUrl, color)
            color
        }
    }.getOrElse {
        Logger.e("CloudImageColor", "Error fetching image color")
        null
    }

    fun fetchImageColor(imageUrl: String?, callback: (color: Int?) -> Unit) {
        val strategy = imageUrl?.let { CloudImageConfig.processor.findStrategy(it) }
        if (imageUrl == null || strategy !is AverageHueUrlStrategy) {
            callback(null)
            return
        }

        ioScope.launch {
            val color = fetchImageColor(imageUrl)
            withContext(Dispatchers.Main) {
                callback(color)
            }
        }
    }

    fun getThumbnail(imageUrl: String?): String {
        return process(imageUrl, ImageFormat.JPEG, Resolution.LOW)
    }

    fun getHighQualityThumbnail(imageUrl: String?): String {
        return process(imageUrl, ImageFormat.JPEG, Resolution.HD)
    }

    @JvmStatic
    fun asWebp(imageUrl: String?): String {
        if (imageUrl == null || !ImageFormat.PNG.`is`(imageUrl)) return imageUrl.orEmpty()
        return process(imageUrl, ImageFormat.WEBP)
    }

    private fun process(
        imageUrl: String?,
        format: ImageFormat,
        resolution: Resolution? = null
    ): String {
        if (imageUrl == null) return ""
        return CloudImageConfig.processor.process(
            imageUrl,
            ImageProcessingOptions(format, resolution)
        )
    }
}
