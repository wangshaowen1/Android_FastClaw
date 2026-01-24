package com.kgmedia.fastfilm.feature.payment.vip

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.common.utils.DateUtil
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
import com.kgmedia.fastfilm.core.network.apis.entity.VipConfigInfo
import com.kgmedia.fastfilm.core.network.apis.request.MemberPaymentBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.payment.pay.PaymentViewModel
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.kuaige.payment.PaymentClient
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:会员权益
 * @Data: 2025/4/21  9:34
 * @Version: 1.0
 * *****************************************
 */
class MembershipBenefitsViewModel : PaymentViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        title = "会员中心",
        navigationAction = { back() },
        background = Color.TRANSPARENT,
    )
    val loadState by lazy { ObservableInt() }
    val menuAction = MutableLiveData(false)

    //权益配置
    val vipConfigInfo = MutableLiveData<VipConfigInfo?>()
    var selectedConfig: VipConfigInfo.MembershipConfigurationInfo? = null
        set(value) {
            field = value
            updateButton()
        }

    //用户头像
    val userAvatar = ObservableField<String>(User.get().avatar)

    //用户昵称
    val userNickname = ObservableField<String>(User.get().nickname)

    //会员状态文案
    val memberStatus = ObservableField("暂未开通会员，开通即享会员特权")

    //底部按钮文字
    val buttonText = ObservableField("立即开通")
    val buttonEnabled = ObservableBoolean(false)

    //会员图标
    val memberIcon = ObservableField(R.drawable.ic_vip_expired54)

    //会员说明
    val desc = ObservableField("")

    //当前支付项
    var paymentTerms: VipConfigInfo.MembershipConfigurationInfo? = null

    //当前支付方式
    var payment: PaymentClient.PaymentMethod? = null


    override fun onCreate() {
        super.onCreate()
        updateMemberStatus()
        //权益配置
        loadConfig()
    }

    override fun onResume() {
        super.onResume()
        updateMemberStatus()
        checkBaoFuResult {
            refreshVipInfo("购买成功")
        }
    }

    /**
     * 权益配置
     */
    fun loadConfig() {
        if (vipConfigInfo.value?.items.isNullOrEmpty()) loadState.set(LoadStateViewHolder.LOADING)
        launch({
            response(
                bean = mRepo.api.vipConfig(UserTokenBody()),
                result = {
                    vipConfigInfo.value = data
                    updateLoadState()
                }, error = {
                    this?.msg?.showToast()
                    updateLoadState()
                })
        }, onError = {
            parseMessage(it)?.showToast()
            updateLoadState()
        })
    }

    private fun updateLoadState() {
        if (vipConfigInfo.value?.items.isNullOrEmpty()) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    /**
     * 支付
     */
    fun startPayment(
        paymentMethod: PaymentClient.PaymentMethod,
        vipConfigId: String,
    ) {
        if (vipConfigId.isEmpty()) return
        payment = paymentMethod
        pay(
            payment = paymentMethod,
            dataSource = { paymentMethod, requestBody, responseSender ->
                launchWithLoading {
                    val rightsWechatPaymentBody = MemberPaymentBody(
                        configId = vipConfigId,
                    )
                    if (paymentMethod == PaymentClient.PaymentMethod.WxPay) {
                        //微信支付
                        response(mRepo.api.memberWechatOrdering(rightsWechatPaymentBody)) {
                            setupBaoFuData(data)
                            responseSender?.sendData(
                                this.data?.toPaymentParams(
                                    paymentMethod,
                                    this?.data?.isBaoFu == true
                                )
                            )
                        }
                    } else if (paymentMethod == PaymentClient.PaymentMethod.Alipay) {
                        //支付宝支付
                        response(mRepo.api.memberAlipayOrdering(rightsWechatPaymentBody)) {
                            responseSender?.sendData(this.data?.toPaymentParams(paymentMethod))
                        }
                    }
                }
            },
            requestBody = MemberPaymentBody(
                configId = vipConfigId
            ),
            onPaid = {
                //支付成功
                refreshVipInfo("购买成功")
            },
            onFailed = { error, paymentData -> })
    }

    /**
     * 支付成功获取会员信息
     */
    fun refreshVipInfo(toast: String? = null) {
        val user = User.get()
        val vipStatus = user.vipStatus
        val vipType = user.vipType
        val vipExpiredTime = user.vipExpiredTime
        val maxTimes = 3
        launchWithLoading {
            var times = 0
            while (times < maxTimes) {
                delay(1000)
                runCatching {
                    withContext(Dispatchers.IO) { mRepo.api.getUserHomeInfo(UserTokenBody()).data }?.let {
                        User.get().updateFrom(it)
                        updateMemberStatus()
                        if (user.vipStatus != vipStatus || user.vipType != vipType || user.vipExpiredTime != vipExpiredTime) {
                            times = maxTimes
                            toast?.showToast()

                            val allPayMoneyYuan = if (paymentTerms?.saleVipAmount == null) {
                                "0"
                            } else {
                                paymentTerms?.saleVipAmount?.div(1000f)?.formatMoney()
                            }
                            val allPayType = when (payment) {
                                PaymentClient.PaymentMethod.WxPay -> "weixinpay"
                                PaymentClient.PaymentMethod.Alipay -> "alipay"
                                else -> ""
                            }

                            EventManager.onAppTrack(
                                AppEvent.JG_BUY_VIP,
                                mapOf(
                                    GrowingIO.ALL_PAY_MONEY_YUAN to allPayMoneyYuan,
                                    GrowingIO.JG_VIP_TYPE to User.get().vipType.toString(),
                                    GrowingIO.ALL_PAY_TYPE to allPayType,
                                )
                            )
                            FlowEventBus.postSync(Events.PurchaseEvent(Events.PurchaseEvent.VIP))
                        }
                    }
                }
                times++
                if (times < maxTimes) {
                    delay(2000)
                }
            }
        }
    }

    private fun updateMemberStatus() {
        val user = User.get()
        if (user.isVip) {
            //vip类型：1-初级会员 2-中级会员 3-高级会员
            when (user.vipType) {
                1 -> memberStatus.set(
                    "当前为初级会员，到期时间:${
                        DateUtil.format(
                            User.get().vipExpiredTime ?: 0L,
                            DateUtil.FORMAT_DATE
                        )
                    }"
                )

                2 -> memberStatus.set(
                    "当前为中级会员，到期时间:${
                        DateUtil.format(
                            User.get().vipExpiredTime ?: 0L,
                            DateUtil.FORMAT_DATE
                        )
                    }"
                )

                3 -> memberStatus.set("当前为高级会员")
            }
            memberIcon.set(R.drawable.ic_vip54)
        } else if (user.isExpiredVip) {
            memberStatus.set("会员已到期，立即续费享受会员特权")
            memberIcon.set(R.drawable.ic_vip_expired54)
        } else {
            memberStatus.set("暂未开通会员，开通即享会员特权")
            memberIcon.set(null)
        }
        updateButton()
    }

    private fun updateButton() {
        val user = User.get()
        if (user.isVip) {
            val selectedVipType = selectedConfig?.vipType ?: 0
            when (user.vipType) {
                3 -> {
                    buttonText.set("已开通会员")
                    buttonEnabled.set(false)
                }

                selectedVipType -> {
                    buttonText.set("立即续费")
                    buttonEnabled.set(true)
                }

                else -> {
                    buttonText.set("已开通会员")
                    buttonEnabled.set(false)
                }
            }
        } else {
            buttonText.set("立即开通")
            buttonEnabled.set(true)
        }
    }
}