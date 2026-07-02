package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 更新在线简历属性请求体
 *
 * @param attributeGroupCode 属性组编码
 * @param inputMode 输入方式
 * @param value 属性值
 */
data class OnlineResumeAttributeUpdateBody(
    val attributeGroupCode: String?,
    val inputMode: String?,
    val value: Any?,
)
