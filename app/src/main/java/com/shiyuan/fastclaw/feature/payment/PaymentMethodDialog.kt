package com.shiyuan.fastclaw.feature.payment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constant
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.databinding.DialogPaymentMethodBinding
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.kuaige.payment.PaymentClient
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:支付方式选择弹框
 * @Data: 2025/4/23  13:56
 * @Version: 1.0
 * *****************************************
 */
class PaymentMethodDialog : BaseBottomPushDialogFragment<DialogPaymentMethodBinding>() {

    companion object {

        //创作者权益
        const val CREATOR_RIGHTS = 1

        //支付类型
        const val PAYMENT_TYPE = "paymentType"

        //金额
        const val AMOUNT = "amount"

        /**
         * 验证支付条件，然后后台同时容许支付宝跟微信就弹框，反之直接调用响应可以的支付方式
         * @param manager FragmentManager
         * @return PaymentMethodDialog?
         */
        fun verifyThePaymentConditions(
            manager: FragmentManager,
            onConfirmClick: ((isWechat: Boolean) -> Unit)? = null,
            paymentType: Int? = CREATOR_RIGHTS,
            amount: String? = null
        ): PaymentMethodDialog? {
            //是否支持支付宝
            val isAlipay = Config.get().appConfig.paymentStatus?.alipay?.status == 1
            //是否支持微信
            val isWechat = Config.get().appConfig.paymentStatus?.wxpay?.status == 1
            if (isAlipay && isWechat) {
                val dialog = show(manager = manager, paymentType = paymentType, amount = amount)
                dialog.onConfirmClick = onConfirmClick
                return dialog
            }
            if (isWechat) {
                onConfirmClick?.invoke(true)
                return null
            }
            onConfirmClick?.invoke(false)
            return null
        }


        fun show(
            manager: FragmentManager,
            paymentType: Int? = CREATOR_RIGHTS,
            amount: String? = null
        ): PaymentMethodDialog {
            val dialog = PaymentMethodDialog()
            dialog.arguments = Bundle().apply {
                putInt(PAYMENT_TYPE, paymentType ?: CREATOR_RIGHTS)
                putString(AMOUNT, amount)
            }
            dialog.show(manager)
            return dialog
        }
    }

    //当前选择的是微信(true)还是支付宝(false)
    private val wechat = AtomicBoolean(false)

    //支付方式
    private var paymentType = CREATOR_RIGHTS

    //支付金额
    private var amount: String? = null

    private val wayAdapter by lazy {
        QuickAdapter<ItemPaymentMethodVm>(R.layout.item_payment_method, emptyList())
    }

    //点击确认回调
    var onConfirmClick: ((isWechat: Boolean) -> Unit)? = null


    override fun onPrepareView() {
        super.onPrepareView()
        paymentType = arguments?.getInt(PAYMENT_TYPE) ?: CREATOR_RIGHTS
        amount = arguments?.getString(AMOUNT)

        if (amount.isNullOrBlank()) {
            binding.clAmount.visibility = View.GONE
        } else {
            binding.clAmount.visibility = View.VISIBLE
            binding.tvAmount.text = amount
        }

        binding.rvPaymentMethod.apply {
            addItemDecoration(SpaceDecoration(8.dp))
            adapter = wayAdapter
        }

        Config.get().appConfig.paymentStatus?.apply {
            val data = arrayListOf<ItemPaymentMethodVm>()
            val zfb = ItemPaymentMethodVm(
                PaymentMethod(
                    name = R.string.alipay_payment.getResString(),
                    icon = R.drawable.ic_pm_alipay,
                    type = PaymentClient.PaymentMethod.Alipay
                )
            )
            val wx = ItemPaymentMethodVm(
                PaymentMethod(
                    name = R.string.wechat_pay.getResString(),
                    icon = R.drawable.ic_pm_wechat,
                    type = PaymentClient.PaymentMethod.WxPay
                )
            )
            zfb.onItemClick.value = {
                wechat.set(false)
                zfb.select.set(true)
                wx.select.set(false)
            }
            wx.onItemClick.value = {
                wechat.set(true)
                zfb.select.set(false)
                wx.select.set(true)
            }
            if ((alipay?.short ?: 0) >= (wxpay?.short ?: 0)) {
                zfb.select.set(true)
                wechat.set(false)
                data.add(zfb)
                if (Constant.SDKConfiguration.IS_SHOW_WE_CHAT) {
                    data.add(wx)
                }
            } else {
                wx.select.set(true)
                wechat.set(true)
                if (Constant.SDKConfiguration.IS_SHOW_WE_CHAT) {
                    data.add(wx)
                }
                data.add(zfb)
            }
            wayAdapter.submitList(data)
        }

        binding.sivBack.setOnSingleClickListener {
            dismiss()
        }
        binding.stvPayment.setOnSingleClickListener {
            dismiss()
            onConfirmClick?.invoke(wechat.get())
        }
    }
}