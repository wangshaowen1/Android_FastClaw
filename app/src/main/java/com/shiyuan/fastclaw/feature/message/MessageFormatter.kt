package com.shiyuan.fastclaw.feature.message

import com.shiyuan.fastclaw.core.User
import com.tulele.im.custom.CustomAttachment
import com.tulele.im.entity.MessageWrapper
import com.tulele.im.entity.UnknownAttachment
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object MessageFormatter {

    // P2P 单聊会话类型值。
    private const val P2P_CONVERSATION_TYPE = 1

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
     * 拼接 P2P 单聊 conversationId。
     */
    fun buildP2pConversationId(targetId: String): String {
        return "${User.get().imAccessId.orEmpty()}|$P2P_CONVERSATION_TYPE|$targetId"
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
