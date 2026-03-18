package com.kgmedia.fastfilm.feature.splash.newer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.toPaymentParams
import com.kgmedia.fastfilm.core.network.apis.response.FirstRechargeDialogData
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.payment.pay.PaymentViewModel
import com.kgmedia.fastfilm.feature.recharge.PowerRepository
import com.kgmedia.fastfilm.router.AppNavigator
import com.kgmedia.fastfilm.router.AppRouter
import com.kuaige.payment.PaymentClient
import com.nhsd.basic.ext.getParcelableValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FirstRechargeViewModel : PaymentViewModel<PowerRepository>() {

    val data by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, FirstRechargeDialogData::class.java) }


    val hourStr = MutableLiveData("00")
    val minuteStr = MutableLiveData("00")
    val secondStr = MutableLiveData("00")

    private var countDownJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        // 获取到期时间戳
        val invalidTime = data?.popupInvalidTime ?: 0L
        if (invalidTime > 0) {
            startCountDown(invalidTime)
        }
    }

    private fun startCountDown(expireTime: Long) {
        countDownJob?.cancel()
        countDownJob = viewModelScope.launch {
            while (true) {
                val currentTime = System.currentTimeMillis()
                val diffMs = expireTime - currentTime

                if (diffMs <= 0) {
                    // 倒计时结束
                    hourStr.value = "00"
                    minuteStr.value = "00"
                    secondStr.value = "00"

                    countDownJob?.cancel()
                    break
                }

                // 计算时、分、秒
                val totalSeconds = diffMs / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                // 格式化为两位数 (例如 9 -> "09")
                hourStr.value = String.format("%02d", hours)
                minuteStr.value = String.format("%02d", minutes)
                secondStr.value = String.format("%02d", seconds)

                delay(1000)
            }
        }
    }

    override fun onCleared() {
        countDownJob?.cancel()
        super.onCleared()
    }

    fun wxPay() {
        if (AppNavigator.checkLoginIntercepted()) return
        val item = data?.pointConfigVO ?: return
        pay(
            PaymentClient.PaymentMethod.WxPay,
            { paymentMethod, requestBody, responseSender ->
                launchWithLoading {
                    response(mRepo.createPowerOrder(paymentMethod, item.pointConfigCode, null)) {
                        responseSender?.sendData(this.data?.toPaymentParams(paymentMethod))
                    }
                }
            },
            item,
            { paymentData ->
                refreshBalance()
                dismiss()
            },
            { error, paymentData -> })
    }

    private fun refreshBalance() {
        Global.refreshBalance()
    }

}