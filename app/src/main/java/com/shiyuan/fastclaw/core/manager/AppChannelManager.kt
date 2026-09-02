package com.shiyuan.fastclaw.core.manager

import android.content.Context
import com.bytedance.hume.readapk.HumeSDK
import com.kwai.monitor.payload.TurboHelper
import com.tencent.vasdolly.helper.ChannelReaderUtil

/**
 * 宿主应用的分发渠道入口，统一隔离渠道 SDK 与业务 objectId 映射。
 */
object AppChannelManager {

    private const val DEFAULT_CHANNEL = "default"
    private const val DEFAULT_OBJECT_ID = "1"

    private const val CHANNEL_TOUTIAO = "toutiao"
    private const val CHANNEL_KUAISHOU = "kuaishou"
    private const val CHANNEL_BAIDU = "baidu"
    private const val CHANNEL_WANGYI = "wangyi"
    private const val CHANNEL_TENCENT = "tencent"

    @Volatile
    private var resolvedChannel: String = DEFAULT_CHANNEL

    @get:JvmStatic
    val channel: String
        get() = resolvedChannel

    @get:JvmStatic
    val objectId: String
        get() = resolveObjectId(channel)

    @JvmStatic
    fun init(context: Context) {
        resolvedChannel = resolveChannel(context.applicationContext)
    }

    private fun resolveChannel(context: Context): String {
        return try {
            val toutiaoChannel = HumeSDK.getChannel(context)
            if (!toutiaoChannel.isNullOrEmpty() && toutiaoChannel.startsWith(CHANNEL_TOUTIAO)) {
                toutiaoChannel
            } else {
                val kuaishouChannel = TurboHelper.getChannel(context)
                if (!kuaishouChannel.isNullOrEmpty() && kuaishouChannel.startsWith(CHANNEL_KUAISHOU)) {
                    kuaishouChannel
                } else {
                    ChannelReaderUtil.getChannel(context).takeUnless { it.isNullOrEmpty() } ?: DEFAULT_CHANNEL
                }
            }
        } catch (_: Exception) {
            DEFAULT_CHANNEL
        }
    }

    private fun resolveObjectId(channel: String): String {
        if (channel.isEmpty()) {
            return DEFAULT_OBJECT_ID
        }
        return when {
            channel.startsWith(CHANNEL_TOUTIAO) -> "11"
            channel.startsWith(CHANNEL_KUAISHOU) -> "12"
            channel.startsWith(CHANNEL_BAIDU) -> "13"
            channel.startsWith(CHANNEL_WANGYI) -> "14"
            channel.startsWith(CHANNEL_TENCENT) -> "15"
            else -> when (channel) {
                "qq" -> "2"
                "huawei" -> "3"
                "xiaomi" -> "4"
                "vivo" -> "5"
                "oppo" -> "6"
                "baidu" -> "7"
                "qihu360", "qh360" -> "8"
                "xsc" -> "9"
                "rongyao" -> "10"
                "admin" -> "1000"
                else -> DEFAULT_OBJECT_ID
            }
        }
    }
}
