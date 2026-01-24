package com.kgmedia.fastfilm.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author xiaoowuu
 * <br>
 * 用户钱包信息
 * </pre>
 * <br>
 */
@Parcelize
data class WalletInfo(
    //积分余额
    val pointBalance: Int? = 0,
) : Parcelable