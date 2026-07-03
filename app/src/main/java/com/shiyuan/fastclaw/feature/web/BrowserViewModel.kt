package com.shiyuan.fastclaw.feature.web

import android.app.Activity
import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.toPaymentParams
import com.shiyuan.fastclaw.core.model.H5PayInfo
import com.shiyuan.fastclaw.core.model.H5PayStatusInfo
import com.shiyuan.fastclaw.feature.web.data.BrowserRepository
import com.kuaige.payment.PaymentClient
import com.kuaige.payment.PaymentResultStrategy
import com.kuaige.payment.entity.PaymentParams
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.getResColor

class BrowserViewModel : AppRepositoryViewModel<BrowserRepository>() {

    val title by lazy { mBundle.getString(Constants.Extra.TITLE) }
    val url by lazy { mBundle.getString(Constants.Extra.URL) }
    val interceptBack by lazy { mBundle.getBoolean(Constants.Extra.INTERCEPT_BACK) }
    val darkStyle by lazy { mBundle.getBoolean(Constants.Extra.DARK_STYLE) }
    val isLoading = MutableLiveData(false)
    val isLoadError = MutableLiveData(false)

    lateinit var paymentClient: PaymentClient
    var orderNo: String? = null

    val mToolbarVM by lazy {
        ToolbarViewModel(
            navigationAction = { back() },
            navigationDrawable = if (darkStyle) R.drawable.icon_back_left.getDrawable() else R.drawable.ic_nav_back_black.getDrawable(),
            title = title,
            titleColor = if (darkStyle) R.color.color_1a1a1a.getResColor() else R.color.color_121212.getResColor(),
            background = if (darkStyle) Color.TRANSPARENT else Color.WHITE
        )
    }
    val openWebUrl = MutableLiveData<String?>(null)
    val payStatusCallback = MutableLiveData<H5PayStatusInfo?>(null)
    fun startPay(info: H5PayInfo) {
        val launchPay = {
            val isZfb = "zfb".equals(info.payWay, true)
            val payment = if (isZfb) PaymentClient.PaymentMethod.Alipay
            else PaymentClient.PaymentMethod.WxPay
            paymentClient.setDataSource { _, _, responseSender ->
                launchWithLoading(true, {
                    val response = if (isZfb) {
                        mRepo.h5PayAlipay(info.payInfo)
                    } else {
                        mRepo.h5PayWx(info.payInfo)
                    }
                    if (response.isSuccess) {
                        val data = response.data
                        orderNo = data?.orderNo
                        data?.payOrder?.let {
                            responseSender?.sendData(it.toPaymentParams(payment))
                        } ?: responseSender?.sendError(null)
                    } else {
                        responseSender?.sendError(null)
                    }
                }, onError = { responseSender?.sendError(it) })
            }
            paymentClient.setResultStrategy(object : PaymentResultStrategy() {
                override fun doWithSuccess(context: Activity?, method: PaymentClient.PaymentMethod?, paymentParams: PaymentParams?) {
                    payStatusCallback.value = H5PayStatusInfo(200, orderNo)
                }

                override fun doWithFailure(context: Activity?, method: PaymentClient.PaymentMethod?, paymentParams: PaymentParams?, e: Throwable?) {
                    payStatusCallback.value = H5PayStatusInfo(400, orderNo)
                }
            })
            paymentClient.startPayment(payment, info)
        }
        launchPay.invoke()
//        setLoading(true)
//        launch(
//            block = {
//                //注销状态，0未申请，1提交申请
//                val removeAccountState = withContext(Dispatchers.IO) { mRepo.api.logOffStatus(UserTokenBody()).data?.removeAccountState }
//                setLoading(false)
//                if (removeAccountState == 1) {
//                    topActivity?.showCommonDialog(
//                        title = "确认是否购买",
//                        tip = "当前账号正在申请注销中，确认购买将自动取消注销账号",
//                        confirm = {
//                            launch { response(mRepo.api.cancelLogOff(UserTokenBody())) {} }
//                            launchPay.invoke()
//                        },
//                        cancel = {
//                            payStatusCallback.value = H5PayStatusInfo(400, orderNo)
//                        }
//                    )
//                } else {
//                    launchPay.invoke()
//                }
//            },
//            onError = {
//                setLoading(false)
//                launchPay.invoke()
//            }
//        )
    }
}
