package com.shiyuan.fastclaw.feature.clawagent.message

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * agent对话消息类
 * @createtime 2026年05月25日 14:40
 * @author TooWhiteT
 */
@Parcelize
data class ClawAgentMessageInfo(
    /**
     * 唯一id
     */
    val id: String,

    /**
     * 消息类型
     */
    val messageType: Int,

    /**
     * 发送方
     */
    val sendObj: String,

    /**
     * 接收方
     */
    val receiveObj: String,

    /**
     * 消息内容
     */
    val content: String? = null,

    /**
     * 消息元数据，是原始消息[JobAgentMessage]的[payloadJson]赋值
     */
    val metaData: String? = null,//等于payloadJson

    val messagePhase: String? = null,

    val groupStatus: String? = null,

    val displayPolicy: String? = null,

    val replaceTargetMessageId: String? = null,

    /**
     * 是否重新回答
     */
    var isReplyAnswer: Boolean = false
): Parcelable
