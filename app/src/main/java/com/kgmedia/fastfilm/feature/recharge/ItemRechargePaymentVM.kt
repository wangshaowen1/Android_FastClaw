package com.kgmedia.fastfilm.feature.recharge

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kuaige.payment.PaymentClient


/**
 * 充值支付方式
 */
class ItemRechargePaymentVM(paymentMethod: PaymentMethod?) : AppItemViewModel<PaymentMethod>(paymentMethod) {
    val isChecked by lazy { ObservableBoolean(false) }
}

data class PaymentMethod(
    val name: String,
    val icon: Int,
    val type: PaymentClient.PaymentMethod
)