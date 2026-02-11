package com.kgmedia.fastfilm.feature.recharge

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.request.QueryAssetBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.payment.pay.PaymentViewModel
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.ext.showToast

class PowerRechargeViewModel : PaymentViewModel<PowerRepository>() {
    /**
     * 账户余额
     */
    val balance by lazy { ObservableField<String>() }
    val loadState by lazy { ObservableInt() }
    val powerAdapter by lazy { QuickAdapter<ItemRechargePowerVM>(R.layout.item_recharge_power, emptyList()) }
    val paymentAdapter by lazy { QuickAdapter<ItemRechargePaymentVM>(R.layout.item_recharge_payment, emptyList()) }
    private var currentPower: ItemRechargePowerVM? = null
    private var currentPayment: ItemRechargePaymentVM? = null

    override fun onCreate() {
        super.onCreate()
        loadConfig()
    }

    override fun onResume() {
        super.onResume()
        setUserInfo()
        fetchUserInfo()
    }

    fun loadConfig() {
        if (powerAdapter.items.isEmpty()) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                response(
                    bean = mRepo.api.workAssetSceneList(QueryAssetBody(null, null, null, null, null)),
                    result = {
//                        data?.diamondConfigInfo
//                            ?.map { ItemRechargeKCoinVM(it, true).apply { onItemClick.value = { select() } } }
//                            ?.let { vms ->
//                                if (firstRechargeSelected.get().not()) {
//                                    vms.firstOrNull { (it.data.get() as? RechargeKCoinConfigData.DiamondConfig)?.defaultSelectStatus == 1 }?.select()
//                                }
//                                adapter.submitList(vms)
//                                adapter2.submitList(vms)
//                            }
                        updateLoadState()
                    },
                    error = { updateLoadState() }
                )
            },
            onError = {
                updateLoadState()
            })
    }

    private fun updateLoadState() {
        if (powerAdapter.items.isEmpty()) {
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
        val payment = currentPayment?.data?.get()?.type
        if (payment == null) {
            "请选择支付方式".showToast()
            return
        }
        pay(
            payment,
            { paymentMethod, requestBody, responseSender ->
//                launchWithLoading {
//                    response(mRepo.recharge(paymentMethod, item, from, popupId)) {
//                        responseSender?.sendData(this.data?.toPaymentParams(paymentMethod))
//                    }
//                }
            },
            item,
            { paymentData ->
                fetchUserInfo()
            },
            { error, paymentData -> })
    }

    fun viewPolicy() {
        val activity = topActivity ?: return
        val url = Config.get().appConfig.algorithmUrl ?: return
        SimpleBrowserActivity.launch(activity, url)
    }

    private fun setUserInfo() {
        val user = User.get()
        balance.set(user.phoneNum)
    }

    private fun fetchUserInfo() {
        if (User.get().isLogged) {
            launch {
                response(mRepo.getUserInfo()) {
                    User.get().updateFrom(data)
                    setUserInfo()
                }
            }
        }
    }

}