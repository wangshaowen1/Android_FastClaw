package com.kgmedia.fastfilm.feature.payment.vip

import android.annotation.SuppressLint
import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.core.network.apis.entity.VipConfigInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:会员权益适配器Vm
 * @Data: 2025/4/17  14:40
 * @Version: 1.0
 * *****************************************
 */
class ItemMembershipBenefitsVm(membershipConfigurationInfo: VipConfigInfo.MembershipConfigurationInfo?) :
    AppItemViewModel<VipConfigInfo.MembershipConfigurationInfo>(membershipConfigurationInfo) {

    //是否选中
    val isSelected = ObservableBoolean(false)

    //是否突出
    fun isHighlight(): Boolean {
        return data.get()?.isZoom == 1
    }

    @SuppressLint("DefaultLocale")
    fun membershipAmount(): String {
        //把金额除以1000在格式化成最多保留俩位小数点，如果后面是零就省略
        return data.get()?.vipAmount?.formatMoney() ?: ""
    }

    fun isVipAdditionalIcon(): Boolean {
        return data.get()?.vipAdditionalIcon?.isNotEmpty() == true
    }
}