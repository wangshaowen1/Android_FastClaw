package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 查询线上简历单个普通属性请求体
 *
 * @param attributeGroupCode 属性分组编码
 */
data class OnlineResumeAttributeQueryBody(
    val attributeGroupCode: String?,
)
