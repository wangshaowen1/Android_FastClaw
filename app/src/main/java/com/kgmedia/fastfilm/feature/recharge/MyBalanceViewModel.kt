package com.kgmedia.fastfilm.feature.recharge

import androidx.annotation.LayoutRes
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.toPaymentParams
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.details.ComputingPowerDetailsActivity
import com.kgmedia.fastfilm.feature.payment.pay.PaymentViewModel
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.async

class MyBalanceViewModel(@LayoutRes private val paymentItemLayout: Int) : PaymentViewModel<PowerRepository>() {
    val toolbar by lazy {
        ToolbarViewModel(
            title = "我的余额",
            navigationAction = { back() },
            menuText = "算力明细",
            menuAction = { ComputingPowerDetailsActivity.start(topActivity) }
        )
    }

    /**
     * 账户余额
     */
    val balance by lazy { ObservableField<String>() }
    val loadState by lazy { ObservableInt() }
    val powerAdapter by lazy { QuickAdapter<ItemRechargePowerVM>(R.layout.item_recharge_power, emptyList()) }
    val paymentAdapter by lazy { QuickAdapter<ItemRechargePaymentVM>(paymentItemLayout, emptyList()) }
    private var currentPower: ItemRechargePowerVM? = null
    private var currentPayment: ItemRechargePaymentVM? = null

    override fun onCreate() {
        super.onCreate()
        loadConfig()
    }

    override fun onResume() {
        super.onResume()
        updateBalance()
        refreshBalance()
    }

    fun loadConfig() {
        if (powerAdapter.items.isEmpty() || paymentAdapter.items.isEmpty()) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                val pointConfigListDeferred = async { mRepo.pointConfigList().data }
                val paymentConfigListDeferred = async { mRepo.paymentConfigList().data }
                val pointConfigList = pointConfigListDeferred.await()
                val paymentConfigList = paymentConfigListDeferred.await()
                pointConfigList?.pointBalanceNum?.let { User.get().storeComputingPower(it) }
                pointConfigList?.items?.map { ItemRechargePowerVM(it).apply { onItemClick.value = { select() } } }.let { vms ->
                    powerAdapter.submitList(vms)
                    (vms?.firstOrNull { it.data.get()?.isDefault == 1 } ?: vms?.firstOrNull())?.select()
                }
                paymentConfigList?.items?.map { ItemRechargePaymentVM(it).apply { onItemClick.value = { select() } } }.let { vms ->
                    paymentAdapter.submitList(vms)
                    (vms?.firstOrNull { it.data.get()?.isDefault == 1 } ?: vms?.firstOrNull())?.select()
                }
                updateLoadState()
            },
            onError = {
                updateLoadState()
            })
    }

    private fun updateLoadState() {
        if (powerAdapter.items.isEmpty() || paymentAdapter.items.isEmpty()) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    private fun ItemRechargePowerVM.select() {
        currentPower?.isChecked?.set(false)
        isChecked.set(true)
        currentPower = this
    }

    private fun ItemRechargePaymentVM.select() {
        currentPayment?.isChecked?.set(false)
        isChecked.set(true)
        currentPayment = this
    }

    fun recharge() {
        val item = currentPower?.data?.get()
        if (item == null) {
            "请选择充值金额".showToast()
            return
        }
        val payment = currentPayment?.data?.get()
        val paymentMethod = payment?.paymentMethod
        if (paymentMethod == null) {
            "请选择支付方式".showToast()
            return
        }
        pay(
            paymentMethod,
            { paymentMethod, requestBody, responseSender ->
                launchWithLoading {
                    response(mRepo.createPowerOrder(paymentMethod, item.pointConfigCode, payment.payMerchantConfigId)) {
                        responseSender?.sendData(this.data?.toPaymentParams(paymentMethod))
                    }
                }
            },
            item,
            { paymentData ->
                refreshBalance()
            },
            { error, paymentData -> })
    }

    fun viewPolicy() {
        val activity = topActivity ?: return
        val url = Config.get().appConfig.clientPaymentProtocolUrl ?: return
        SimpleBrowserActivity.launch(activity, url)
    }

    fun updateBalance() {
        balance.set(User.get().computingPower.toString())
    }

    private fun refreshBalance() {
        Global.refreshBalance()
    }

}