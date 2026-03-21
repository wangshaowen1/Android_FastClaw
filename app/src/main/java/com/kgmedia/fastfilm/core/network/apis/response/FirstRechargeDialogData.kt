package com.kgmedia.fastfilm.core.network.apis.response

import android.os.Parcelable
import com.kgmedia.fastfilm.core.network.apis.entity.PointConfigInfo
import kotlinx.parcelize.Parcelize

@Parcelize
data class FirstRechargeDialogData(
    val extraGiftText: String? = null,
    val giftTag: String? = null,
    val imageUrl: String? = null,
    val needShow: Boolean? = null,
    val popupInvalidTime: Long? = null,
    val pricePointText: String? = null,
    val pointConfigVO: PointConfigInfo? = null,
) : Parcelable {
    fun isNeedShow(): Boolean {
        return needShow == true
    }
}