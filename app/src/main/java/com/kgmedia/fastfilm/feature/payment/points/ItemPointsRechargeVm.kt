package com.kgmedia.fastfilm.feature.payment.points

import android.annotation.SuppressLint
import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.response.ScoreConfigData

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:积分充值
 * @Data: 2025/4/17  14:40
 * @Version: 1.0
 * *****************************************
 */
class ItemPointsRechargeVm(
    data: ScoreConfigData.PointsData?
) : AppItemViewModel<ScoreConfigData.PointsData>(data) {

    //是否选中
    val select = ObservableField(false)

    @SuppressLint("DefaultLocale")
    fun membershipAmount(): String{
        // 把金额除以1000，并格式化成最多保留两位小数，去掉末尾的0
        val amount = data.get()?.pointAmount?.div(1000.0)
        val decimalFormat = java.text.DecimalFormat("#.##")
        return "¥${amount?.let { decimalFormat.format(it) }}"
    }
}