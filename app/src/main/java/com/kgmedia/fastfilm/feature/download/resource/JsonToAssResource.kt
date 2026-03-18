package com.kgmedia.fastfilm.feature.download.resource

import android.text.format.DateUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ResourceUtils
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.kuaige.videoedit.resource.Resource
import java.util.Locale

/**
 * 字幕转换（JSON -> ASS）
 */
class JsonToAssResource(private val storyboardId: String, private val json: String, private val assTemplate: String = "lyrics_template.ass") : Resource() {

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
        "ass_${storyboardId}_${json.hashCode()}.ass"
    }

    override suspend fun doWork(onProgress: ((Int) -> Unit)?) {
        val type = object : TypeToken<List<SubtitleItem>>() {}.type
        val subtitleList: List<SubtitleItem> = GsonUtils.fromJson(json, type)
        if (subtitleList.isEmpty()) {
            return
        }
        // 读取 ASS 模板
        val subtitle = runCatching {
            val template = ResourceUtils.readAssets2String(assTemplate)
//                .replace("#PlayResX#", vw.toString())
//                .replace("#PlayResY#", vh.toString())
//                .replace("#Fontsize#", config.subtitleTextSize.toString())
//                .replace("#MarginV#", config.subtitleMarginBottom.toString())
            StringBuilder().appendLine(template)
        }.getOrNull() ?: throw IllegalStateException("Assets 中缺少 lyrics_template.ass")

        // 遍历生成字幕行
        subtitleList.forEachIndexed { index, item ->
            subtitle.appendLine("Dialogue: 0,${item.startTime?.toAssTime()},${item.endTime?.toAssTime()},Karaoke,${item.text}")
            // 简单的进度计算
            if (index % 5 == 0) {
                onProgress?.invoke((index * 100f / subtitleList.size).toInt())
            }
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
     * 毫秒转 ASS 时间格式 (H:MM:SS.mm)
     */
    private fun Int.toAssTime(): String {
        val hours = this / DateUtils.HOUR_IN_MILLIS
        val remaining = this % DateUtils.HOUR_IN_MILLIS
        val minutes = remaining / DateUtils.MINUTE_IN_MILLIS
        val seconds = (remaining % DateUtils.MINUTE_IN_MILLIS) / DateUtils.SECOND_IN_MILLIS
        val millis = (remaining % 1000) / 10 // ASS 使用百分秒 (cs)

        return String.format(Locale.getDefault(), "%d:%02d:%02d.%02d", hours, minutes, seconds, millis)
    }
}