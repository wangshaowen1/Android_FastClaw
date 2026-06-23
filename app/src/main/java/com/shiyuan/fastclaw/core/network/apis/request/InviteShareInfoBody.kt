package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 查询邀请分享信息
 * @param inviteScene 邀请场景，默认海报
 */
data class InviteShareInfoBody(
    val inviteScene: String = INVITE_SCENE_POSTER
) : UserTokenBody() {
    companion object {
        const val INVITE_SCENE_POSTER = "POSTER"
    }
}
