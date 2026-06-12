package com.shiyuan.fastclaw.core.network.apis.entity

import kotlinx.serialization.Serializable

/**
 * @desc：城市、岗位、行业、福利偏好通用消息体
 * @author：CaoWei
 * @date：2026/6/12 10:34
 */
@Serializable
data class SimpleMsgAgent (
    val code: String? = null,
    val name: String? = null
)