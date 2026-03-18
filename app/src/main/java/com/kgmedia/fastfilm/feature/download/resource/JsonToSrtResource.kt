package com.kgmedia.fastfilm.feature.download.resource

import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.kuaige.videoedit.resource.Resource
import java.util.Locale

/**
 * 字幕转换（JSON -> SRT）
 */
class JsonToSrtResource(private val storyboardId: String, private val json: String) : Resource() {

    // 定义JSON对应的实体类
    data class SubtitleItem(
        val text: String?,
        @SerializedName("start_time")
        val startTime: Int?,
        @SerializedName("end_time")
        val endTime: Int?,
        val words: List<WordItem>?
    )

    data class WordItem(
        val text: String?,
        @SerializedName("start_time")
        val startTime: Int?,
        @SerializedName("end_time")
        val endTime: Int?
    )

    // 生成一个唯一的文件名
    override val filename by lazy {
        "ass_${storyboardId}_${json.hashCode()}.srt"
    }

    override suspend fun doWork(onProgress: ((Int) -> Unit)?) {
        val type = object : TypeToken<List<SubtitleItem>>() {}.type
        val subtitleList: List<SubtitleItem> = GsonUtils.fromJson(json, type)
        if (subtitleList.isEmpty()) {
            return
        }
        val subtitle = StringBuilder()
        // 遍历生成字幕行
        subtitleList.forEachIndexed { index, item ->
            // 1. 序号 (从1开始)
            subtitle.append("${index + 1}\n")

            // 2. 时间轴 (格式: 00:00:00,000 --> 00:00:00,000)
            val startTimeStr = formatTime(item.startTime ?: 0)
            val endTimeStr = formatTime(item.endTime ?: 0)
            subtitle.append("$startTimeStr --> $endTimeStr\n")

            // 3. 文本内容
            subtitle.append("${item.text}\n\n")
        }

        // 写入文件
        val saved = FileIOUtils.writeFileFromString(file, subtitle.toString())
        if (saved) {
            onProgress?.invoke(100)
        } else {
            throw Exception("歌词文件写入失败 ${file.name}")
        }
    }

    /**
     * 毫秒转 SRT 时间格式: 00:00:00,000
     */
    private fun formatTime(ms: Int): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60)) % 24
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}