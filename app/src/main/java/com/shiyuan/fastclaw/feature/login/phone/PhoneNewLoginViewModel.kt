package com.shiyuan.fastclaw.feature.login.phone

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.T
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.common.ext.isRealMobile
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.loginToast
import com.shiyuan.fastclaw.core.ext.parseMessage
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.network.apis.response.UserHomeData
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.login.data.FlashLoginRepository
import com.shiyuan.fastclaw.kit.SignUtils
import com.nhsd.basic.bus.FlowEventBus
import com.shiyuan.fastclaw.core.network.apis.response.CheckMobileBindStatusData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/**
 * 新版验证码登录页
 *  @author：CaoWei
 *  @date：2026/6/30 15:49
 */
class PhoneNewLoginViewModel : AppRepositoryViewModel<FlashLoginRepository>() {
    /**
     * 用户是否已勾选登录协议。
     */
    val checkedPolicy = ObservableBoolean(false)

    /**
     * 手机号输入页和验证码页共用的手机号。
     */
    val mobile = MutableLiveData<String?>()

    /**
     * 验证码页输入的 4 位短信验证码。
     */
    val smsCode = MutableLiveData<String?>()

    /**
     * 获取验证码按钮是否可点击。
     */
    val sendCodeEnable = MutableLiveData(false)

    /**
     * 重新获取验证码文案，倒计时期间显示剩余秒数。
     */
    val sendCodeText = ObservableField(R.string.send_sms_code.getResString())

    /**
     * 是否处于验证码倒计时中。
     */
    val codeCounting = MutableLiveData(false)

    /**
     * 当前倒计时任务标识，用于丢弃旧倒计时回调。
     */
    private var countdownId = 0L

    /**
     * 校验手机号是否可用于获取验证码，并刷新按钮可点击态。
     */
    fun checkSendCodeEnable() {
        sendCodeEnable.postValue(mobile.value.isRealMobile())
    }

    /**
     * 发送登录短信验证码。
     *
     * @param startCountdownOnSuccess 成功后是否立即启动倒计时
     * @param bizType 短信业务类型
     * @param onSuccess 验证码发送成功后的页面回调
     * @param onError 验证码发送失败后的页面回调
     */
    fun sendSmsCode(bizType: Int, startCountdownOnSuccess: Boolean = true, onSuccess: () -> Unit, onError: ((String?) -> Unit)? = null) {
        val phone = mobile.value
        if (!phone.isRealMobile()) {
            onError?.invoke(R.string.login_new_phone_error.getResString())
            return
        }
        codeCounting.postValue(true)
        sendCodeEnable.postValue(false)
        launch(
            block = {
                response(
                    mRepo.sendSmsValidateCode(phone, bizType),
                    result = {
                        T.showShort(R.string.get_validate_code_success)
                        if (startCountdownOnSuccess) {
                            startCountdown()
                        }
                        onSuccess()
                    },
                    error = {
                        onError?.invoke(this?.msg)
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
     * 获取验证码前校验手机号绑定状态。
     *
     * @param scene 校验场景，1-绑定手机号，2-换绑手机号
     * @param bindTicket 三方登录返回的绑定票据，仅绑定手机号场景使用
     * @param onAvailable 手机号可用时继续执行
     */
    fun checkMobileBindStatus(scene: Int, bindTicket: String?, onSuccess: (CheckMobileBindStatusData?) -> Unit, onError: ((String?) -> Unit)? = null) {
        val phone = mobile.value
        if (!phone.isRealMobile()) {
            onError?.invoke(R.string.login_new_phone_error.getResString())
            return
        }
        launchWithLoading(

            block = {
                response(
                    mRepo.checkMobileBindStatus(phone, scene, bindTicket),
                    result = {
                        onSuccess(data)
                    },
                    error = {
                        onError?.invoke(this?.msg)
                    }
                )
            },
            onError = {
                parseMessage(it)?.showToast()
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
        val smsCode = smsCode.value
        if (!phone.isRealMobile() || smsCode?.length != Constants.SMS_CODE_LENGTH) {
            showToast(R.string.enter_right_validate_code)
            onError?.invoke()
            return
        }
        KeyboardUtils.hideSoftInput(ActivityUtils.getTopActivity())
        launchWithLoading(false, block = {
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
    fun loginByThird(loginType: Int, map: MutableMap<String, String>?, onLogin: ((Boolean, UserHomeData?) -> Unit)?) {
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
                val userData = data
                if (userData?.bindMobile == 1) {// 已绑定手机号
                    saveLoginUser(userData, { _ ->
                        onLogin?.invoke(false, userData)
                    })
                } else {// 未绑定
                    onLogin?.invoke(true, userData)
                }

            }, error = { this?.msg?.showToast() })
        }, onError = { showToast(R.string.toast_sign_in_fail) })
    }

    /**
     * 三方登录后绑定手机号，成功后再保存三方登录返回的用户信息。
     */
    fun bindPhone(userData: UserHomeData?, onLogin: ((UserHomeData?) -> Unit)?, onError: (() -> Unit)? = null) {
        val phone = mobile.value
        val validateCode = smsCode.value
        if (!phone.isRealMobile()) {
            showToast(R.string.login_new_phone_error)
            onError?.invoke()
            return
        }
        if (validateCode?.length != Constants.SMS_CODE_LENGTH) {
            showToast(R.string.enter_right_validate_code)
            onError?.invoke()
            return
        }
        launchWithLoading(false, block = {
            response(
                mRepo.bindMobile(phone, validateCode, userData?.bindTicket),
                result = {
                    saveLoginUser(data, onLogin, phone)
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
     * 修改手机号场景绑定新手机号。
     */
    fun changePhone(onSuccess: (() -> Unit)?, onError: (() -> Unit)? = null) {
        val phone = mobile.value
        val validateCode = smsCode.value
        if (!phone.isRealMobile()) {
            showToast(R.string.login_new_phone_error)
            onError?.invoke()
            return
        }
        if (validateCode?.length != Constants.SMS_CODE_LENGTH) {
            showToast(R.string.enter_right_validate_code)
            onError?.invoke()
            return
        }
        launchWithLoading(false, block = {
            response(mRepo.changeMobile(phone, validateCode), {
                showToast(R.string.bind_success)
                User.get().storePhoneNum(phone)
                FlowEventBus.postSync(Events.BindingSuccessfulEvent())
                onSuccess?.invoke()
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
     * 保存登录用户并发送登录成功事件。
     */
    private fun saveLoginUser(userData: UserHomeData?, onLogin: ((UserHomeData?) -> Unit)?, boundPhone: String? = null) {
        User.get().updateFrom(userData)
        if (!boundPhone.isNullOrBlank()) {
            User.get().storePhoneNum(boundPhone)
        }
        SignUtils.signIn()
        userData.loginToast()
        onLogin?.invoke(userData)
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
