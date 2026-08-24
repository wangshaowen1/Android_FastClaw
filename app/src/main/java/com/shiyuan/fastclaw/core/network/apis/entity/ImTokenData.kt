package com.shiyuan.fastclaw.core.network.apis.entity

/**
 * 云信 IM Token 刷新结果。
 * @param ready 云信账号是否就绪
 * @param baseAccountId 当前主体基础账号 ID
 * @param subjectType 主体类型：1-候选人，2-企业 HR
 * @param imAccId 云信账号 ID
 * @param imAccToken 云信登录 token
 */
data class ImTokenData(
    var ready: Boolean? = null,
    var baseAccountId: Long? = null,
    var subjectType: Int? = null,
    var imAccId: String? = null,
    var imAccToken: String? = null
)
