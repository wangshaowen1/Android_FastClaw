package com.kgmedia.fastfilm.feature.payment.points

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.rv.InfiniteQuickAdapter
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.toPaymentParams
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.core.network.apis.request.DanmakuBody
import com.kgmedia.fastfilm.core.network.apis.request.OrderingPointsBody
import com.kgmedia.fastfilm.core.network.apis.request.PointConfigBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorRepository
import com.kgmedia.fastfilm.feature.payment.pay.PaymentViewModel
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kuaige.payment.PaymentClient
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.delay

/**
 * 首次充值弹窗
 */
class FirstTimeRechargeViewModel : PaymentViewModel<ShortDramaEditorRepository>() {
    private val dramasId by lazy { mBundle.getString(Constants.Extra.DRAMAS_ID) }
    val loading by lazy { ObservableBoolean() }
    val adapter by lazy { QuickAdapter<ItemFirstTimeRechargeVM>(R.layout.item_first_time_recharge, emptyList()) }
    val sampleAdapter by lazy { InfiniteQuickAdapter<ItemFirstTimeRechargeSampleVM>(R.layout.item_first_time_recharge_sample, emptyList()) }
    val selectedItem by lazy { ObservableField<ItemFirstTimeRechargeVM>() }
    val payment by lazy { ObservableField(PaymentClient.PaymentMethod.WxPay) }
    val cost by lazy { ObservableField<CharSequence>() }
    private var protocolUrl: String? = null
//    private val originalBalance = User.get().scoreBalance

    override fun onCreate() {
        super.onCreate()
        getDanmakuList()
        loadConfig()
        val points = mBundle.getInt(Constants.Extra.COST)
        if (points > 0) {
            applyCost(points)
        } else {
            calcCost()
        }
    }

    private fun getDanmakuList() {
        launch {
            response(mRepo.api.danmakuList(DanmakuBody(1, 1, 100))) {
                data?.items?.map { ItemFirstTimeRechargeSampleVM(it) }?.let { sampleAdapter.submitList(it) }
            }
        }
    }

    private fun calcCost() {
        launch {
            response(mRepo.calculatePrice(dramasId)) {
                data?.pointAmount?.let { applyCost(it) }
            }
        }
    }

    private fun applyCost(points: Int) {
        cost.set("解锁后续图片还需${points}积分")
    }

    /**
     * 刷新
     */
    fun loadConfig() {
        if (loading.get()) return
        loading.set(true)
        launch(block = {
            response(
                mRepo.api.scoreConfig(PointConfigBody(1)),
                result = result@{
                    protocolUrl = data?.protocolUrl
                    data?.items?.map {
                        ItemFirstTimeRechargeVM(it).apply {
                            onItemClick.value = { select() }
                        }
                    }?.let { adapter.submitList(it) }
                    adapter.items.firstOrNull { it.data.get()?.defaultSelectStatus == 1 }.select()
                    loading.set(false)
                },
                error = {
                    loading.set(false)
                })
        }, onError = {
            loading.set(false)
        })
    }

    private fun ItemFirstTimeRechargeVM?.select() {
        if (selectedItem.get() == this) return
        selectedItem.get()?.selected?.set(false)
        this?.selected?.set(true)
        selectedItem.set(this)
    }

    fun viewPolicy() {
        protocolUrl?.let { SimpleBrowserActivity.launch(topActivity, it) }
    }

    fun changePayment(method: PaymentClient.PaymentMethod) {
        payment.set(method)
    }

    fun recharge() {
        val configId = selectedItem.get()?.data?.get()?.configId ?: return

        val allPayType = when (payment.get()) {
            PaymentClient.PaymentMethod.WxPay -> "weixinpay"
            PaymentClient.PaymentMethod.Alipay -> "alipay"
            else -> ""
        }
        EventManager.onAppTrack(
            AppEvent.JG_CLICK_POPUP_UNLOCK_WORK_RECHARGE_UNLOCK_BUTTON,
            GrowingIO.ALL_PAY_MONEY_YUAN to selectedItem.get()?.data?.get()?.pointAmount?.formatMoney(),
            GrowingIO.ALL_PAY_TYPE to allPayType,
        )

        pay(
            payment.get(),
            { paymentMethod, requestBody, responseSender ->
                loading.set(true)
                launch(
                    block = {
                        val body = OrderingPointsBody(configId, 1, dramasId)
                        response(
                            if (paymentMethod == PaymentClient.PaymentMethod.Alipay) mRepo.api.alipayOrderingPoints(body)
                            else mRepo.api.wechatOrderingPoints(body),
                            result = {
                                loading.set(false)
                                val baoFu = data.isBaoFu()
                                if (baoFu) {
                                    setupBaoFuData(this.data)
                                }
                                responseSender?.sendData(this.data?.toPaymentParams(paymentMethod, baoFu))
                            },
                            error = {
                                loading.set(false)
                            })
                    },
                    onError = {
                        loading.set(false)
                    }
                )
            },
            "",
            { paymentData -> refreshBalance() },
            { error, paymentData -> })
    }

    override fun onResume() {
        super.onResume()
        checkBaoFuResult {
            refreshBalance()
        }
    }

    private fun refreshBalance() {
        loading.set(true)
        launch(block = {
            var times = 0
            while (times < 3) {
                delay(1000)
                runCatching {
                    mRepo.api.getUserHomeInfo(UserTokenBody()).data?.let {
                        User.get().updateFrom(it)
//                        if (User.get().scoreBalance > originalBalance) {
//                            rechargeSuccess()
//                            return@launch
//                        }
                    }
                }
                times++
            }
            rechargeSuccess()
        }, onError = {
            loading.set(false)
        })
    }

    private fun rechargeSuccess() {
        "支付成功".showToast()
        dismiss()
        FlowEventBus.postSync(Events.ShortDramasUpdatedEvent(dramasId))
    }
}