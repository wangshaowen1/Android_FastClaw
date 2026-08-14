package com.shiyuan.fastclaw.core.ext

import com.google.gson.Gson
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo
import com.tulele.im.entity.Contact

/**
 * AIMarry 的云信用户扩展，目前仅承载业务用户 ID。
 */
data class AimarryImUserExtension(
    val userId: String? = null,
)

private val imUserExtensionGson = Gson()

/**
 * 按 AIMarry 协议解析联系人扩展；扩展为空或格式异常时返回 null。
 */
fun Contact.aimarryExtension(): AimarryImUserExtension? {
    val json = extensionJson?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        imUserExtensionGson.fromJson(json, AimarryImUserExtension::class.java)
    }.getOrNull()
}

/**
 * 按 AIMarry 协议解析云信用户扩展；扩展为空时返回 null。
 */
fun NimUserInfo.aimarryExtension(): AimarryImUserExtension? {
    val userId = extensionMap
        ?.get(USER_ID_KEY)
        ?.toString()
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return AimarryImUserExtension(userId)
}

/**
 * 解析联系人业务用户 ID：扩展字段优先，其次使用明确传入值，最后解析 u_账号。
 */
fun Contact.aimarryUserId(explicitUserId: String? = null): Long? {
    return aimarryExtension()?.userId.toPositiveUserId()
        ?: explicitUserId.toPositiveUserId()
        ?: id.aimarryFallbackUserId()
}

/**
 * 解析云信用户业务 ID：扩展字段优先，u_账号仅作为最终兜底。
 */
fun NimUserInfo.aimarryUserId(): Long? {
    return aimarryExtension()?.userId.toPositiveUserId()
        ?: account.aimarryFallbackUserId()
}

/**
 * 从 u_数字 格式的云信账号解析业务用户 ID，仅供最终兜底。
 */
fun String?.aimarryFallbackUserId(): Long? {
    return this
        ?.takeIf { it.startsWith(IM_ACCOUNT_USER_PREFIX) }
        ?.removePrefix(IM_ACCOUNT_USER_PREFIX)
        .toPositiveUserId()
}

/** 将文本转换为有效的正数业务用户 ID。 */
private fun String?.toPositiveUserId(): Long? = this?.toLongOrNull()?.takeIf { it > 0L }

private const val USER_ID_KEY = "userId"
private const val IM_ACCOUNT_USER_PREFIX = "u_"
