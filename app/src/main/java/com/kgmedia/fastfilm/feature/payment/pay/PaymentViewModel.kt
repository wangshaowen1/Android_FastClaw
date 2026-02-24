package com.kgmedia.fastfilm.feature.payment.pay

import android.app.Activity
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.createAppPayment
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.PaymentData
import com.kgmedia.fastfilm.core.network.apis.request.OrderStatusBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response
import com.kuaige.payment.PaymentClient
import com.kuaige.payment.PaymentDataSource
import com.kuaige.payment.PaymentResultStrategy
import com.kuaige.payment.entity.PaymentParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean


/**
 * 支付方式及发起支付封装
 * @author xiaoowu
 */
open class PaymentViewModel<T : AppRepository> : AppRepositoryViewModel<T>() {
    private var paymentClient: PaymentClient = createAppPayment()

    /**
     * 支付
     * @param payment 支付方式
     */
    fun <Body> pay(
        payment: PaymentClient.PaymentMethod?,
        dataSource: PaymentDataSource<*, *>,
        requestBody: Body,
        onPaid: ((paymentData: PaymentParams?) -> Unit)?,
        onFailed: ((error: Throwable?, paymentData: PaymentParams?) -> Unit)?
    ) {
        val launchPay = {
            paymentClient
                .setDataSource(dataSource)
                ?.setResultStrategy(object : PaymentResultStrategy() {
                    override fun doWithSuccess(
                        context: Activity?,
                        method: PaymentClient.PaymentMethod?,
                        paymentParams: PaymentParams?
                    ) {
                        onPaid?.invoke(paymentParams)
                    }

                    override fun doWithFailure(
                        context: Activity,
                        method: PaymentClient.PaymentMethod?,
                        paymentParams: PaymentParams?,
                        e: Throwable?
                    ) {
                        super.doWithFailure(context, method, paymentParams, e)
                        onFailed?.invoke(e, paymentParams)
                    }
                })
                ?.startPayment(payment, requestBody)
        }
        setLoading(true)
        launch(
            block = {
                //注销状态，0未申请，1提交申请
                val removeAccountState = withContext(Dispatchers.IO) { mRepo.api.logOffStatus(UserTokenBody()).data?.removeAccountState }
                setLoading(false)
                if (removeAccountState == 1) {
                    topActivity?.showCommonDialog(
                        title = "确认是否购买",
                        tip = "当前账号正在申请注销中，确认购买将自动取消注销账号",
                        confirm = {
                            launch { response(mRepo.api.cancelLogOff(UserTokenBody())) {} }
                            launchPay.invoke()
                        },
                        cancel = null
                    )
                } else {
                    launchPay.invoke()
                }
            },
            onError = {
                setLoading(false)
                launchPay.invoke()
            }
        )
    }

    /**
     * 通用轮询任务
     */
    suspend fun <T> pollTask(
        maxRetry: Int = 5,
        delayMs: Long = 500,
        action: suspend () -> T?,
        checkSuccess: (T?) -> Boolean
    ): Boolean {
        repeat(maxRetry) { index ->
            // 使用 runCatching 捕获网络异常，如果报错则 result 为 null
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    action()
                }
            }.getOrNull()

            if (checkSuccess(result)) {
                return true
            }

            // 只有不是最后一次尝试时，才进行延时
            if (index < maxRetry - 1) {
                delay(delayMs)
            }
        }
        return false
    }

    val isBaoFu = AtomicBoolean(false)

    @Volatile
    var baoFuOrderId: String? = null

    fun checkBaoFuResult(failure: (() -> Unit)? = null, success: () -> Unit) {
        if (!isBaoFu.compareAndSet(true, false)) {
            return
        }
        val currentOrderId = baoFuOrderId
        baoFuOrderId = null
        if (currentOrderId.isNullOrEmpty()) {
            // 异常情况：有状态但无ID
            failure?.invoke()
            return
        }
        launch {
            val isSuccess = pollTask(
                maxRetry = 5,
                delayMs = 500, // 每次间隔 500ms
                action = {
                    mRepo.api.getOrderStatus(OrderStatusBody(orderNo = currentOrderId))
                },
                checkSuccess = { response ->
                    response?.data?.status == 1
                }
            )
            if (isSuccess) {
                success.invoke()
            } else {
                failure?.invoke()
            }
        }
    }

    fun setupBaoFuData(data: PaymentData?) {
        baoFuOrderId = data?.orderNo
        isBaoFu.set(data?.isBaoFu == true)
    }
}