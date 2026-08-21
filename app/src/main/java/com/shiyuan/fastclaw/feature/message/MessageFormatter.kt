package com.shiyuan.fastclaw.feature.message

import com.tulele.im.custom.CustomAttachment
import com.tulele.im.entity.MessageWrapper
import com.tulele.im.entity.UnknownAttachment
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object MessageFormatter {
    private const val RESUME_FILE_TYPE_PDF = "PDF"
    private const val RESUME_FILE_TYPE_DOC = "DOC"
    private const val RESUME_FILE_TYPE_DOCX = "DOCX"
    private const val MIME_TYPE_PDF = "application/pdf"
    private const val MIME_TYPE_DOC = "application/msword"
    private const val MIME_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    /**
     * 读取协议自定义消息附件 JSON。
     */
    fun MessageWrapper.customPayload(): JSONObject? {
        val raw = when (val attachment = this.attachment) {
            is UnknownAttachment -> attachment.raw
            is CustomAttachment -> attachment.toJson(false)
            else -> null
        }?.takeIf { it.isNotBlank() } ?: return null
        return try {
            JSONObject(raw)
        } catch (ignored: Exception) {
            null
        }
    }

    /**
     * 将接口返回的文件类型转换为协议使用的 MIME 类型。
     */
    fun String?.toResumeMimeType(): String? = when (this?.uppercase()) {
        RESUME_FILE_TYPE_PDF -> MIME_TYPE_PDF
        RESUME_FILE_TYPE_DOC -> MIME_TYPE_DOC
        RESUME_FILE_TYPE_DOCX -> MIME_TYPE_DOCX
        else -> this
    }

    /**
     * 将消息时间压缩为列表展示文案：今天显示时分，昨天带前缀，其余显示日期。
     */
    fun format(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().also { it.timeInMillis = timestamp }
        return when {
            isSameDay(now, target) -> SimpleDateFormat("H:mm", Locale.getDefault()).format(timestamp)
            isYesterday(now, target) -> "昨天${SimpleDateFormat("H:mm", Locale.getDefault()).format(timestamp)}"
            isSameYear(now, target) -> SimpleDateFormat("M月d日", Locale.getDefault()).format(timestamp)
            else -> SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(timestamp)
        }
    }

    /**
     * 判断两个时间是否处于同一个自然日。
     */
    private fun isSameDay(first: Calendar, second: Calendar): Boolean =
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)

    /**
     * 判断目标时间是否为当前时间的前一个自然日。
     */
    private fun isYesterday(now: Calendar, target: Calendar): Boolean {
        val yesterday = (now.clone() as Calendar).also { it.add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(yesterday, target)
    }

    /**
     * 判断两个时间是否处于同一年，用于选择短日期或完整日期格式。
     */
    private fun isSameYear(first: Calendar, second: Calendar): Boolean =
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
}
