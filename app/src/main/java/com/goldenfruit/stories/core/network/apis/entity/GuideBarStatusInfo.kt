package com.kgmedia.fastfilm.core.network.apis.entity

/**
 * 新用户创作体验引导条
 */
data class GuideBarStatusInfo(
    val guideBarStatus: Int?, //引导条展示状态，0不展示，1展示
    val dramasId: String?
)