package com.kgmedia.fastfilm.feature.recharge

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.PaymentInfo


/**
 * 充值支付方式
 */
class ItemRechargePaymentVM(info: PaymentInfo?) : AppItemViewModel<PaymentInfo>(info) {
    val isChecked by lazy { ObservableBoolean(false) }
}