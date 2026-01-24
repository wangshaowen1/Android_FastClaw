package com.kgmedia.fastfilm.feature.payment.points

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.ext.toPaymentParams
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.core.network.apis.request.OrderingPointsBody
import com.kgmedia.fastfilm.core.network.apis.request.PointConfigBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.network.apis.response.ScoreConfigData
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.payment.pay.PaymentViewModel
import com.kuaige.payment.PaymentClient
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的余额
 * @Data: 2025/9/11/周四  9:24
 * @Version: 1.0
 * *****************************************
 */
class PointsRechargeViewModel : PaymentViewModel<AppRepository>() {
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "我的余额",
        background = Color.TRANSPARENT,
        menuText = "余额明细",
        menuTextColorRes = R.color.color_6e6e6e,
        menuAction = {
            onMenuClick.value = true
        }
    )

    //刷新状态
    val refreshType = ObservableBoolean(false)

    //当前积分
    val points = ObservableField((User.get().walletInfo?.pointBalance ?: 0).toString())

    //菜单点击
    val onMenuClick = MutableLiveData(false)

    //积分充值列表
    val pointsRechargeData = MutableLiveData<ScoreConfigData?>(null)

    //当前支付项
    var paymentTerms: ScoreConfigData.PointsData? = null

    //当前支付方式
    var payment: PaymentClient.PaymentMethod? = null

    override fun onCreate() {
        super.onCreate()
        pointsProgram()
    }

    fun pointsProgram() {
        launch({
            response(
                bean = mRepo.api.scoreConfig(PointConfigBody()),
                result = {
                    pointsRechargeData.value = data
                    refreshType.set(false)
                }, error = {
                    this?.msg?.showToast()
                    refreshType.set(false)
                })
        }, onError = {
            parseMessage(it)?.showToast()
            refreshType.set(false)
        })
    }


    /**
     * 支付
     */
    fun startPayment(
        paymentMethod: PaymentClient.PaymentMethod,
        pointsData: ScoreConfigData.PointsData,
    ) {
        payment = paymentMethod
        paymentTerms = pointsData
        pay(
            payment = paymentMethod,
            dataSource = { paymentMethod, requestBody, responseSender ->
                launchWithLoading {
                    val orderingPointsBody = OrderingPointsBody(
                        configId = pointsData.configId
                    )
                    if (paymentMethod == PaymentClient.PaymentMethod.WxPay) {
                        //微信支付
                        response(mRepo.api.wechatOrderingPoints(orderingPointsBody)) {
                            setupBaoFuData(data)
                            responseSender?.sendData(
                                this.data?.toPaymentParams(
                                    paymentMethod,
                                    data?.isBaoFu == true
                                )
                            )
                        }
                    } else if (paymentMethod == PaymentClient.PaymentMethod.Alipay) {
                        //支付宝支付
                        response(mRepo.api.alipayOrderingPoints(orderingPointsBody)) {
                            responseSender?.sendData(this.data?.toPaymentParams(paymentMethod))
                        }
                    }
                }
            },
            requestBody = OrderingPointsBody(
                configId = pointsData.configId
            ),
            onPaid = {
                //支付成功
                purchaseSuccessful()
            },
            onFailed = { error, paymentData -> })
    }

    override fun onResume() {
        super.onResume()
        checkBaoFuResult {
            purchaseSuccessful()
        }
    }

    private fun purchaseSuccessful() {
        "购买成功".showToast()
        getUserHomeInfo()
    }


    /**
     * 获取用户主页信息接口
     */
    fun getUserHomeInfo() {
        launch {
            response(
                bean = mRepo.api.getUserHomeInfo(UserTokenBody()),
                result = {
                    if (data == null) return@response
                    User.get().updateFrom(data)
                    points.set((data?.userInfo?.walletInfo?.pointBalance ?: 0).toString())

                    val allPayMoneyYuan = if (paymentTerms?.pointAmount == null) {
                        "0"
                    } else {
                        paymentTerms?.pointAmount?.div(1000f)?.formatMoney()
                    }
                    val allPayType = when (payment) {
                        PaymentClient.PaymentMethod.WxPay -> "weixinpay"
                        PaymentClient.PaymentMethod.Alipay -> "alipay"
                        else -> ""
                    }

                    EventManager.onAppTrack(
                        AppEvent.JG_BUY_INTEGRAL,
                        mapOf(
                            GrowingIO.ALL_PAY_MONEY_YUAN to allPayMoneyYuan,
                            GrowingIO.KEY_ALL_COUNT_INT to paymentTerms?.pointNum,
                            GrowingIO.ALL_PAY_TYPE to allPayType,
                        )
                    )
                    FlowEventBus.postSync(Events.PurchaseEvent(Events.PurchaseEvent.POINTS))
                })
        }
    }
}