package com.shiyuan.fastclaw.feature.login.phone

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.RegexUtils
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.T
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.loginToast
import com.shiyuan.fastclaw.core.ext.parseMessage
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.network.apis.request.SendSmsValidateCodeBody
import com.shiyuan.fastclaw.core.network.apis.response.UserHomeData
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.login.data.FlashLoginRepository
import com.shiyuan.fastclaw.kit.SignUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class PhoneNewLoginViewModel : AppRepositoryViewModel<FlashLoginRepository>() {

    val checkedPolicy = ObservableBoolean(false)
    val mobile = MutableLiveData<String?>()
    val code = MutableLiveData<String?>()
    val sendCodeEnable = MutableLiveData(false)
    val sendCodeText = ObservableField(R.string.send_sms_code.getResString())
    val codeCounting = MutableLiveData(false)

    private var countdownId = 0L

    /**
     * 校验手机号是否可用于获取验证码，并刷新按钮可点击态。
     */
    fun checkSendCodeEnable() {
        sendCodeEnable.postValue(RegexUtils.isMobileSimple(mobile.value))
    }

    /**
     * 发送登录短信验证码。
     *
     * @param startCountdownOnSuccess 成功后是否立即启动倒计时
     * @param onSent 验证码发送成功后的页面回调
     */
    fun sendSmsCode(startCountdownOnSuccess: Boolean = true, onSent: () -> Unit) {
        val phone = mobile.value
        if (!RegexUtils.isMobileSimple(phone)) {
            showToast(R.string.login_new_phone_error)
            return
        }
        codeCounting.postValue(true)
        sendCodeEnable.postValue(false)
        launchWithLoading(
            block = {
                response(
                    mRepo.sendSmsValidateCode(phone, SendSmsValidateCodeBody.BIZ_TYPE_LOGIN),
                    result = {
                        T.showShort(R.string.get_validate_code_success)
                        if (startCountdownOnSuccess) {
                            startCountdown()
                        }
                        onSent()
                    },
                    error = {
                        this?.msg?.showToast()
                        stopCountdown()
                    }
                )
            },
            onError = {
                parseMessage(it)?.showToast()
                stopCountdown()
            }
        )
    }

    /**
     * 使用手机号和验证码登录。
     *
     * @param onLogin 登录成功回调
     * @param onError 登录失败或校验失败回调
     */
    fun phoneLogin(onLogin: ((UserHomeData?) -> Unit)?, onError: (() -> Unit)? = null) {
        if (!checkedPolicy.get()) {
            T.showShort(R.string.please_agree_protocol)
            onError?.invoke()
            return
        }
        val phone = mobile.value
        val smsCode = code.value
        if (!RegexUtils.isMobileSimple(phone) || smsCode?.length != Constants.SMS_CODE_LENGTH) {
            showToast(R.string.enter_right_validate_code)
            onError?.invoke()
            return
        }
        KeyboardUtils.hideSoftInput(ActivityUtils.getTopActivity())
        launchWithLoading(block = {
            response(mRepo.loginByPhone(phone, smsCode), {
                User.get().updateFrom(data)
                SignUtils.signIn()
                data.loginToast()
                onLogin?.invoke(data)
            }, error = {
                this?.msg?.showToast()
                onError?.invoke()
            })
        }, onError = {
            parseMessage(it)?.showToast()
            onError?.invoke()
        })
    }

    /**
     * 使用第三方授权信息登录，目前新版入口只接微信登录。
     */
    fun loginByThird(
        loginType: Int,
        map: MutableMap<String, String>?,
        onLogin: ((UserHomeData?) -> Unit)?
    ) {
        setLoading(true)
        launchWithLoading(false, block = {
            val body = LoginByThirdBody(
                loginType = loginType,
                thirdPartyUnionId = map?.get("unionid"),
                thirdPartyOpenId = map?.get("openid"),
                thirdPartyNickName = map?.get("name"),
                thirdPartyAvatar = map?.get("iconurl"),
                inviteAttribution = Config.get().inviteAttribution,
            )
            response(mRepo.loginByThird(body), result = {
                User.get().updateFrom(data)
                SignUtils.signIn()
                data.loginToast()
                onLogin?.invoke(data)
            }, error = { this?.msg?.showToast() })
        }, onError = { showToast(R.string.toast_sign_in_fail) })
    }

    /**
     * 启动 60 秒验证码倒计时。
     */
    fun startCountdown() {
        val remainSeconds = 60
        codeCounting.postValue(true)
        sendCodeEnable.postValue(false)
        updateCountdown(remainSeconds)
        countdownId = System.nanoTime()
        launch {
            val id = countdownId
            flow {
                for (time in remainSeconds - 1 downTo 0) {
                    delay(1000)
                    if (id != countdownId) break
                    emit(time)
                }
            }.collect { updateCountdown(it) }
        }
    }

    /**
     * 更新倒计时秒数，结束时恢复可重新发送状态。
     */
    private fun updateCountdown(seconds: Int) {
        if (seconds <= 0) {
            stopCountdown()
            return
        }
        sendCodeText.set("${seconds}s")
    }

    /**
     * 停止倒计时并重置验证码按钮状态。
     */
    private fun stopCountdown() {
        countdownId = 0
        sendCodeText.set(R.string.send_sms_code.getResString())
        codeCounting.postValue(false)
        checkSendCodeEnable()
    }
}
