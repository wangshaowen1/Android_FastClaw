package com.shiyuan.fastclaw.core.network.apis.request


/**
 * 用户反馈请求体
 *
 * @param problemType 问题类型：1=产品建议，2=功能故障，3=其他问题
 * @param problemDesc 问题描述（最多 500 字）
 * @param imagesUrl 图片 URL（最多 4 张，用英文逗号分割）
 * @param contactMobile 联系方式（手机号）
 */
data class FeedbackBody(
    /** 问题类型：1=产品建议，2=功能故障，3=其他问题 */
    val problemType: Int?,
    /** 问题描述（最多 500 字） */
    val problemDesc: String?,
    /** 图片 URL（最多 4 张，用英文逗号分割） */
    var imagesUrl: String?,
    /** 联系方式（手机号） */
    val contactMobile: String?
) : UserTokenBody()