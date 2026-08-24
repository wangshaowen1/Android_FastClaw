package com.shiyuan.fastclaw.feature.message.data

import com.tencent.mmkv.MMKV

/**
 * IM 会话输入草稿本地存储，只保存当前设备的输入框内容。
 */
object MessageDraftStore {

    // 草稿内容 MMKV key 前缀。
    private const val KEY_TEXT_PREFIX = "message_draft_text_"

    // 草稿存储 MMKV 实例 id。
    private const val DRAFT_MMKV_ID = "MESSAGE_DRAFT"

    // 草稿 MMKV 实例。
    private val mmkv by lazy { MMKV.mmkvWithID(DRAFT_MMKV_ID) ?: MMKV.defaultMMKV() }

    /**
     * 根据会话 id 读取本地草稿。
     */
    fun get(conversationId: String): String? {
        if (conversationId.isBlank()) return null
        return mmkv.decodeString(textKey(conversationId)).orEmpty().takeIf { it.isNotBlank() }
    }

    /**
     * 保存会话输入草稿，内容为空时清空草稿。
     */
    fun save(conversationId: String, content: String) {
        if (conversationId.isBlank() || content.isBlank()) {
            clear(conversationId)
            return
        }
        mmkv.encode(textKey(conversationId), content)
    }

    /**
     * 清空指定会话的本地草稿。
     */
    fun clear(conversationId: String) {
        if (conversationId.isBlank()) return
        mmkv.removeValueForKey(textKey(conversationId))
    }

    /**
     * 生成草稿内容存储 key。
     */
    private fun textKey(conversationId: String): String = "$KEY_TEXT_PREFIX$conversationId"
}
