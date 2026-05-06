package com.shiyuan.fastclaw.feature.payment

import androidx.databinding.ObservableField
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.kuaige.payment.PaymentClient


/**
 * *****************************************
 * 项目名称: 小元音乐圈
 * @Author: SZYMN
 * @Introduce:支付方式选择
 * @Data: 2025/6/11  9:33
 * @Version: 1.0
 * *****************************************
 */
class ItemPaymentMethodVm(paymentMethod: PaymentMethod?) :
    AppItemViewModel<PaymentMethod>(paymentMethod) {
    val select = ObservableField(false)
}

data class PaymentMethod(
    val name: String,
    val icon: Int,
    val type: PaymentClient.PaymentMethod
)