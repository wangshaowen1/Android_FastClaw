package com.kgmedia.fastfilm.core.network.apis.entity

interface SimpleAsset {
    val assetId: String?
    val assetAppearanceId: String?
    val status: Int?
    val assetDesc: String?
    val assetName: String?
    val assetImage: String?
    val showReference: Boolean
}