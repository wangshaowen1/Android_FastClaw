package com.shiyuan.fastclaw.feature.web

import android.app.Activity
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.blankj.utilcode.util.GsonUtils
import com.kuaige.payment.PaymentClient
import com.kuaige.payment.PaymentResultStrategy
import com.kuaige.payment.entity.PaymentParams
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.getResColor
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.PollingState
import com.shiyuan.fastclaw.core.ext.launchPolling
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.toPaymentParams
import com.shiyuan.fastclaw.core.model.H5PayInfo
import com.shiyuan.fastclaw.core.model.H5PayStatusInfo
import com.shiyuan.fastclaw.core.network.apis.request.OrderStatusBody
import com.shiyuan.fastclaw.core.network.apis.response.H5InterviewPassPackageRep
import com.shiyuan.fastclaw.feature.web.data.BrowserRepository
import kotlinx.coroutines.Job

class BrowserViewModel : AppRepositoryViewModel<BrowserRepository>() {

    companion object {
        private const val PAYMENT_STATUS_MAX_RETRY = 5
        private const val PAYMENT_STATUS_POLL_INTERVAL_MS = 500L
    }

    val title by lazy { mBundle.getString(Constants.Extra.TITLE) }
    val url by lazy { mBundle.getString(Constants.Extra.URL) }
    val interceptBack by lazy { mBundle.getBoolean(Constants.Extra.INTERCEPT_BACK) }
    val darkStyle by lazy { mBundle.getBoolean(Constants.Extra.DARK_STYLE) }
    val isLoading = MutableLiveData(false)
    val isLoadError = MutableLiveData(false)

    lateinit var paymentClient: PaymentClient
    var orderNo: String? = null

    fun replacePaymentClient(client: PaymentClient) {
        if (::paymentClient.isInitialized) {
            paymentClient.release()
        }
        paymentClient = client
    }

    val mToolbarVM by lazy {
        ToolbarViewModel(
            navigationAction = { back() },
            navigationDrawable = if (darkStyle) R.drawable.icon_back_left.getDrawable() else R.drawable.icon_back_left.getDrawable(),
            title = title,
            titleColor = if (darkStyle) R.color.color_1a1a1a.getResColor() else R.color.color_121212.getResColor(),
            background = if (darkStyle) Color.TRANSPARENT else Color.WHITE
        )
    }
    val openWebUrl = MutableLiveData<String?>(null)
    val payStatusCallback = MutableLiveData<H5PayStatusInfo?>(null)
    var data: H5InterviewPassPackageRep? = null
    private var paymentStatusPollingJob: Job? = null

    fun startPay(info: H5PayInfo) {
        paymentStatusPollingJob?.cancel()
        paymentStatusPollingJob = null
        setLoading(false)
        val launchPay = {
            val isZfb = "zfb".equals(info.payWay, true)
            val payment = if (isZfb) PaymentClient.PaymentMethod.Alipay
            else PaymentClient.PaymentMethod.WxPay
            paymentClient.setDataSource { _, _, responseSender ->
                launchWithLoading(true, {
                    val payInfoBody = info.payInfoBody()
                    val response = if (isZfb) {
                        mRepo.h5PayAlipay(payInfoBody)
                    } else {
                        mRepo.h5PayWx(payInfoBody)
                    }
                    if (response.isSuccess) {
                        data = response.data
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
                    if (data?.goodsCode == H5InterviewPassPackageRep.CLEARANCE_PACKAGE) {
                        payStatusCallback.value = H5PayStatusInfo(200, data?.packageId)
                    } else  if (data?.goodsCode == H5InterviewPassPackageRep.SALARY_IS_UNDERESTIMATED || data?.goodsCode == H5InterviewPassPackageRep.ONE_REPLIED_RESUME) {
                        // pollPaymentStatus()
                        payStatusCallback.value = H5PayStatusInfo(200, data?.orderNo)
                    }else {
                        payStatusCallback.value = H5PayStatusInfo(200, data?.orderNo)
                    }
                   // payStatusCallback.value = H5PayStatusInfo(200, User.get().userInfoStr)
                   // payStatusCallback.value = H5PayStatusInfo(200, "{\"goodsName\":\"面试通关包\",\"orderNo\":\"2026070317454005114103344\",\"packageId\":\"66580780191911\"}")
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

    private fun pollPaymentStatus() {
        val currentOrderNo = data?.orderNo
        if (currentOrderNo.isNullOrBlank()) {
            payStatusCallback.value = H5PayStatusInfo(400, currentOrderNo)
            return
        }

        var retryCount = 0
        paymentStatusPollingJob?.cancel()
        setLoading(true)
        paymentStatusPollingJob = launchPolling(
            intervalMs = PAYMENT_STATUS_POLL_INTERVAL_MS,
            retryOnNetworkError = false,
            showToastError = true,
            apiBlock = {
                mRepo.api.getOrderStatus(OrderStatusBody(orderNo = currentOrderNo))
            },
            checkState = { response ->
                retryCount++
                when {
                    response?.data?.tradeStatus == 1 -> PollingState.SUCCESS
                    retryCount >= PAYMENT_STATUS_MAX_RETRY -> PollingState.FAIL
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = {
                paymentStatusPollingJob = null
                setLoading(false)
                payStatusCallback.value = H5PayStatusInfo(200, currentOrderNo)
            },
            onFail = { _, _ ->
                paymentStatusPollingJob = null
                setLoading(false)
                payStatusCallback.value = H5PayStatusInfo(400, currentOrderNo)
            }
        )
    }

    override fun onDestroy() {
        paymentStatusPollingJob?.cancel()
        paymentStatusPollingJob = null
        setLoading(false)
        if (::paymentClient.isInitialized) {
            paymentClient.release()
        }
        super.onDestroy()
    }
}
