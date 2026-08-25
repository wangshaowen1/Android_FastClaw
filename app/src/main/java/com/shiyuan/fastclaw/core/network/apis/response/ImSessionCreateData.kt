package com.shiyuan.fastclaw.core.network.apis.response

/** 创建 App 端 IM 会话结果。 */
data class ImSessionCreateData(
    // 本地会话业务 ID。
    val sessionId: String? = null,
    // 本次是否首次创建。
    val created: Boolean? = false,
    // 本次请求是否切换了会话当前岗位。
    val jobSwitched: Boolean? = false,
    // 会话记录中的发起方基础账号 ID。
    val baseAccountId: Long = 0,
    // 会话记录中的目标方基础账号 ID。
    val targetBaseAccountId: Long = 0,
    // 当前沟通岗位 ID。
    val currentJobId: Long = 0,
    // 当前用户云信账号 ID。
    val currentImAccId: String? = null,
    // 对端云信账号 ID。
    val targetImAccId: String? = null,
    // 对端主体类型：1-候选人，2-企业 HR。
    val targetSubjectType: Int = 0,
    // 对端昵称。
    val targetNickname: String? = null,
    // 对端头像地址。
    val targetAvatarUrl: String? = null,
    // 岗位卡片发送结果。
    val jobCardSendResult: ImJobCardSendResultData? = null,
)

/** 创建 IM 会话后岗位卡片发送结果。 */
data class ImJobCardSendResultData(
    // 后端生成的跨系统消息 ID。
    val messageClientId: String? = null,
    // 云信服务端消息 ID。
    val messageServerId: Long = 0,
    // 是否复用短时成功结果。
    val reused: Boolean? = false,
)
