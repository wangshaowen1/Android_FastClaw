package com.shiyuan.fastclaw.feature.mine.viewModel

import android.graphics.Color
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.parseMessage
import com.shiyuan.fastclaw.core.ext.toPaymentParams
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.entity.PointConfigInfo
import com.shiyuan.fastclaw.core.network.apis.request.TradePayCreateBody
import com.shiyuan.fastclaw.core.network.apis.entity.SchemeData
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.network.apis.response.SimpleBannerInfo
import com.shiyuan.fastclaw.core.network.apis.response.UnderstandingScore
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.mine.UserPointPagingSource
import com.shiyuan.fastclaw.feature.payment.pay.PaymentViewModel
import com.kuaige.payment.PaymentClient
import com.umeng.commonsdk.debug.UMLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的
 * @Data: 2025/4/18  14:10
 * @Version: 1.0
 * *****************************************
 */
class MineViewModel : PaymentViewModel<AppRepository>() {

    var onMenuClick: (() -> Unit)? = null

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.mine_shrimp_record.getResString(),
        background = Color.TRANSPARENT,
        menuAction = { view ->
            onMenuClick?.invoke()
        }
    )

    val mToolbarVMShrimpRecharge = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.mine_sticks.getResString(),
        menuText = R.string.mine_shrimp_record.getResString(),
        background = Color.TRANSPARENT,
        menuAction = { view ->
            onMenuClick?.invoke()
        }
    )

    val mToolbarVMUnderstands = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.shrimp_understands.getResString(),
        background = Color.TRANSPARENT,
        menuAction = { view ->
            onMenuClick?.invoke()
        }
    )


    //是否登录
    val isLogin by lazy { MutableLiveData(false) }

    //用户资料Map
    val userInfo by lazy { ObservableField<Map<String, String?>>() }

    //没有登录不让进
    val noLogIn = MutableLiveData(false)

    /**
     * 账户余额
     */
    val balance by lazy { ObservableField<String>() }
    val workCourseBanner by lazy { ObservableField<SimpleBannerInfo>() }

    /**
     * 账户虾条
     */
    val userPoints = MutableLiveData<String>("0")

    val rechargePackages = MutableLiveData<List<PointConfigInfo>>()

    var underStandingScore = MutableLiveData<UnderstandingScore>()

    /**
     * 虾条记录
     */
    val userPointListFlow = Pager(
        PagingConfig(
            pageSize = 10,
            initialLoadSize = 10
        )
    ) {
        UserPointPagingSource()
    }.flow
        .flowOn(Dispatchers.Default)
        .cachedIn(viewModelScope)

    override fun onCreate() {
        super.onCreate()
        refreshUserInfo()
        //更新必要信息
        updateInformation()
    }

    /**
     * 刷新用户信息
     */
    fun refreshUserInfo() {
        User.Companion.get().let { user ->
            isLogin.postValue(!user.isVisitor)
            if (user.isVisitor) {
                notLoggedIn()
                return
            }
            logOn()
        }
    }


    /**
     * 更新必要信息
     */
    fun updateInformation() {
        //获取用户主页信息接口
//        getUserHomeInfo()
        // 获取虾条信息接口
        getUserPoints()
        understandingScore()
    }

    /**
     * 获取用户主页信息接口
     */
    private fun getUserHomeInfo() {
        User.Companion.get().let { user ->
            isLogin.postValue(!user.isVisitor)
            if (user.isVisitor) {
                notLoggedIn()
                return
            }
            noLogIn.value = false
//            Global.refreshBalance()
            launch {
                response(
                    bean = mRepo.api.getUserHomeInfo(UserTokenBody()),
                    result = {
                        if (data == null) return@response
                        User.Companion.get().updateFrom(data)
                        logOn()
                    })
            }
        }
    }

    /**
     * 获取积分信息(虾条)
     */
    private fun getUserPoints() {
        val user = User.Companion.get()
        user.token?.let {
            launch {
                response(
                    bean = mRepo.api.getUserPoints(),
                    result = {
                        if (data == null) return@response
                        userPoints.postValue("${data.pointsBalance}")
                    }
                )
            }
        }
    }

    fun getRechargePackages() {
        val user = User.Companion.get()
        user.token?.let {
            launch {
                response(
                    bean = mRepo.api.queryPointConfigList(),
                    result = {
                        rechargePackages.postValue(data?.pointConfigList.orEmpty())
                    }
                )
            }
        }
    }

    fun rechargeShrimp(payment: PaymentClient.PaymentMethod, goodsCode: Long) {
        val payBody = TradePayCreateBody(goodsCode = goodsCode)
        pay(
            payment = payment,
            dataSource = { _, _, responseSender ->
                launchWithLoading(true, {
                    val response = if (payment == PaymentClient.PaymentMethod.Alipay) {
                        mRepo.api.createAppAlipayOrder(payBody)
                    } else {
                        mRepo.api.createAppWechatPayOrder(payBody)
                    }
                    response.data?.takeIf { response.isSuccess }?.let { data ->
                        setupBaoFuData(data)
                        responseSender?.sendData(data.toPaymentParams(payment))
                    } ?: responseSender?.sendError(null)
                }, onError = {
                    responseSender?.sendError(it)
                })
            },
            requestBody = payBody,
            onPaid = {
                if (isBaoFu.get()) {
                    checkBaoFuResult(
                        failure = { R.string.mine_recharge_pay_failed.getResString().showToast() },
                        success = { onRechargePaid() }
                    )
                } else {
                    onRechargePaid()
                }
            },
            onFailed = { _, _ ->
                R.string.mine_recharge_pay_failed.getResString().showToast()
            }
        )
    }

    private fun onRechargePaid() {
        R.string.pay_success.getResString().showToast()
        getUserPoints()
    }


    private fun understandingScore() {
        val user = User.Companion.get()
        user.token?.let {
            launch(block = {
                response(
                    ApiFactory.apiService.understandingScore(),
                    result = {
                        underStandingScore.postValue(this.data)
                    },
                    error = {
                        this?.msg?.showToast()
                    })
            }, onError = {
                parseMessage(it)?.showToast()
            }, onComplete = {
                setLoading(false)
            })
        }
    }


    private fun notLoggedIn() {
        userInfo.set(
            mapOf(
                "id" to "",
                "nickname" to "点击登录",
                "avatar" to "",
            )
        )
        noLogIn.value = true
        balance.set("0")
        userPoints.value = "0"
        underStandingScore.value = UnderstandingScore(0)
        workCourseBanner.set(Config.Companion.get().appConfig.workCourseBanner)
    }

    private fun logOn() {
        val user = User.Companion.get()
        userInfo.set(
            mapOf(
                "id" to user.userUid,
                "nickname" to user.nickName,
                "avatar" to user.avatarUrl,
            )
        )
        balance.set(user.computingPower.toString())
        workCourseBanner.set(if (user.isPurchasedCourse()) null else Config.Companion.get().appConfig.workCourseBanner)
    }
}
