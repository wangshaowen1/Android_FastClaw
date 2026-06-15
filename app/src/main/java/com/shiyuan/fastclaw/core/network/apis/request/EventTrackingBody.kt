package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 事件埋点请求体
 *
 * @param eventCode 事件编码
 * @param bizId 业务 ID
 * @param pageLinking 页面链路
 * @param customData 自定义数据（JSON 格式）
 */
data class EventTrackingBody(
    val eventCode: String,
    val bizId: String?,
    val pageLinking: String?,
    val customData: String?
) : UserTokenBody()