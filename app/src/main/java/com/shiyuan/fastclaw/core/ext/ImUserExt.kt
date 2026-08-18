package com.shiyuan.fastclaw.core.ext

import com.google.gson.Gson
import com.tulele.im.entity.Contact
import com.tulele.im.entity.ImUser

/**
 * FastClaw 的云信用户扩展，目前仅承载业务用户 ID。
 */
data class FastClawImUserExtension(
    val userId: String? = null,
)

private val imUserExtensionGson = Gson()

/**
 * 按 FastClaw 协议解析联系人扩展；扩展为空或格式异常时返回 null。
 */
fun Contact.fastClawExtension(): FastClawImUserExtension? {
    val json = extensionJson?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        imUserExtensionGson.fromJson(json, FastClawImUserExtension::class.java)
    }.getOrNull()
}

/**
 * 按 FastClaw 协议解析组件用户扩展；扩展为空或格式异常时返回 null。
 */
fun ImUser.fastClawExtension(): FastClawImUserExtension? {
    val json = extension?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        imUserExtensionGson.fromJson(json, FastClawImUserExtension::class.java)
    }.getOrNull()
}

/**
 * 解析联系人业务用户 ID：扩展字段优先，其次使用明确传入值，最后解析 u_账号。
 */
fun Contact.fastClawUserId(explicitUserId: String? = null): Long? {
    return fastClawExtension()?.userId.toPositiveUserId()
        ?: explicitUserId.toPositiveUserId()
        ?: id.fastClawFallbackUserId()
}

/**
 * 解析云信用户业务 ID：扩展字段优先，u_账号仅作为最终兜底。
 */
fun ImUser.fastClawUserId(): Long? {
    return fastClawExtension()?.userId.toPositiveUserId()
        ?: account.fastClawFallbackUserId()
}

/**
 * 从 u_数字 格式的云信账号解析业务用户 ID，仅供最终兜底。
 */
fun String?.fastClawFallbackUserId(): Long? {
    return this
        ?.takeIf { it.startsWith(IM_ACCOUNT_USER_PREFIX) }
        ?.removePrefix(IM_ACCOUNT_USER_PREFIX)
        .toPositiveUserId()
}

/** 将文本转换为有效的正数业务用户 ID。 */
private fun String?.toPositiveUserId(): Long? = this?.toLongOrNull()?.takeIf { it > 0L }

private const val IM_ACCOUNT_USER_PREFIX = "u_"
