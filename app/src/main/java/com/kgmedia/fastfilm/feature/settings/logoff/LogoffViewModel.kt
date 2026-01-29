package com.kgmedia.fastfilm.feature.settings.logoff

import android.graphics.Color
import android.os.CountDownTimer
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.LogOffStatusInfo
import com.kgmedia.fastfilm.core.network.apis.request.LogoffBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: 小元音乐圈
 * @Author: SZYMN
 * @Introduce:注销账号页面
 * @Data: 2025/4/22  13:55
 * @Version: 1.0
 * *****************************************
 */
class LogoffViewModel : AppRepositoryViewModel<AppRepository>() {
    companion object {
        //注销状态未申请，不可点击状态
        const val REMOVE_ACCOUNT_STATE_NOT_APPLY = 0

        //注销状态未申请，可点击状态
        const val REMOVE_ACCOUNT_STATE_APPLY = 1

        //注销状态已申请，不可点击状态
        const val REMOVE_ACCOUNT_STATE_APPLY_NOT_CLICK = 2
    }

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "注销账号",
        background = Color.TRANSPARENT
    )
    val checkedPolicy = ObservableField(false)

    //注销账户的状态 0未申请，1提交申请
    val removeAccountState = ObservableInt(REMOVE_ACCOUNT_STATE_NOT_APPLY)

    //提交申请文案
    val submitApplyText = ObservableField("申请注销")
    private var countDownTimer: CountDownTimer? = null
    var firstRequest = true
    override fun onStart() {
        super.onStart()
        logOffStatus()
        countdown()
    }

    private fun logOffStatus() {
        launchWithLoading(firstRequest) {
            response(mRepo.api.logOffStatus(UserTokenBody())) {
                if (data?.removeAccountState == LogOffStatusInfo.SUBMIT_APPLY) {
                    removeAccountState.set(REMOVE_ACCOUNT_STATE_APPLY_NOT_CLICK)
                    submitApplyText.set("申请注销中")
                } else {
                    removeAccountState.set(REMOVE_ACCOUNT_STATE_NOT_APPLY)
                    submitApplyText.set("申请注销")
                    //开启倒计时
                    countdown()
                }
                firstRequest = false
            }
        }
    }

    private fun countdown() {
        // 取消之前的倒计时（如果存在）
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) + 1
                submitApplyText.set("申请注销(${seconds}s)")
            }

            override fun onFinish() {
                submitApplyText.set("申请注销")
                removeAccountState.set(REMOVE_ACCOUNT_STATE_APPLY)
                countDownTimer=null
            }
        }.start()
    }

    fun checkPolicy() {
        checkedPolicy.set(checkedPolicy.get() != true)
    }

    fun logOff() {
        startActivity(ApplyLogOffActivity::class.java)
    }

    /**
     * 取消注销
     */
    fun cancelLogoff() {
        launchWithLoading {
            response(mRepo.api.cancelLogOff(UserTokenBody())) {
                "取消成功".showToast()
                finish()
            }
        }
    }


    /**
     * 提交注销申请
     */
    fun commitLogoff(content: String) {
        launchWithLoading {
            response(mRepo.api.logOff(LogoffBody(content))) {
                "若7日内未取消注销申请，则7日后注销成功。".showToast()
                finish()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}