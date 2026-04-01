package com.kgmedia.fastfilm.core.network.apis.entity

data class TabEntity(
    val title: String,
    var badgeCount: Int // 这个值用于动态更新
)