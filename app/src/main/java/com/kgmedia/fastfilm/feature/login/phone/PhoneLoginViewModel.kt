package com.kgmedia.fastfilm.feature.login.phone

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.RegexUtils
import com.chuanglan.shanyan_sdk.OneKeyLoginManager
import com.google.gson.Gson
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.getInstall
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.loginToast
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.network.apis.entity.FlashTestPreFetchNumberInfo
import com.kgmedia.fastfilm.core.network.apis.entity.FlashTestTokenInfo
import com.kgmedia.fastfilm.core.network.apis.request.LoginByThirdBody
import com.kgmedia.fastfilm.core.network.apis.request.SendSmsValidateCodeBody
import com.kgmedia.fastfilm.core.network.apis.response.UserHomeData
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.login.data.FlashLoginRepository
import com.kgmedia.fastfilm.feature.login.phone.PhoneLoginActivity.Companion.TAG
import com.kgmedia.fastfilm.kit.SignUtils
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import com.nhsd.basic.utils.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:闪验登录验证
 * @Data: 2026/2/10/周二  17:10
 * @Version: 1.0
 * *****************************************
 */
class PhoneLoginViewModel : AppRepositoryViewModel<FlashLoginRepository>() {
    companion object {
        //显示加载中
        const val SHOW_LOADING = 0

        //显示一键登录
        const val SHOW_ONE_KEY_LOGIN = 1

        //显示验证码登录
        const val SHOW_VERIFY_CODE_LOGIN = 2
    }

    //当前显示的状态
    val showState = ObservableInt(SHOW_LOADING)

    //加载弹框
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    //闪验取到的手机号信息
    val flashTestPreFetchNumber = ObservableField<FlashTestPreFetchNumberInfo?>()

    val checkedPolicy = ObservableBoolean(false)

    //协议状态变更
    val checkedPolicyChange = MutableLiveData(false)

    //登录结果
    val loginResult = MutableLiveData(false)

    //进来是登录的还是绑定的
    val isBind = ObservableBoolean(false)

    //手机号
    val mobile = MutableLiveData<String?>()

    //发送验证码按钮是否可点击
    val sendCodeEnable = ObservableBoolean(true)

    //发送验证码按钮文字
    val sendCodeText = ObservableField(R.string.send_sms_code.getResString())

    //验证码
    val code by lazy { MutableLiveData<String?>() }

    private var countdownId = 0L

    override fun onCreate() {
        super.onCreate()
        isBind.set(mBundle.getBoolean(PhoneLoginActivity.BIND, false))
        checkedPolicy.set(mBundle.getBoolean(PhoneLoginActivity.CHECKED_POLICY, false))
        if (isBind.get()) {
            showState.set(SHOW_VERIFY_CODE_LOGIN)
        } else {
            //初始化闪验并获取内容
            initFlashTest()
        }
    }

    /**
     * 初始化闪验
     */
    private fun initFlashTest() {
        promptGenerating.value = true to "加载中..."
        //第一步初始化闪验SDK
        OneKeyLoginManager.getInstance().apply {
            //预取号
            getPhoneInfo { code, result ->
                promptGenerating.value = false to ""
                Logger.i(TAG, "预取号 getPhoneInfo code=$code,result=$result")
                //code 为 1022：成功；其他：失败 result 预取号结果描述
                if (1022 != code) {
                    checkedPolicyChange.value = true
                    //跳转验证码登录
                    showState.set(SHOW_VERIFY_CODE_LOGIN)
                    return@getPhoneInfo
                }
                val flashTestPreFetchNumberInfo = runCatching {
                    Gson().fromJson(result, FlashTestPreFetchNumberInfo::class.java)
                }.getOrNull()
                if (flashTestPreFetchNumberInfo == null) {
                    //跳转验证码登录
                    showState.set(SHOW_VERIFY_CODE_LOGIN)
                } else {
                    showState.set(SHOW_ONE_KEY_LOGIN)
                    flashTestPreFetchNumber.set(flashTestPreFetchNumberInfo)
                }
                checkedPolicyChange.value = true
            }
        }
    }


    /**
     * 闪验登录获取token
     */
    fun flashLoginVerification() {
        promptGenerating.value = true to "加载中..."
        OneKeyLoginManager.getInstance().startAuthentication({ code, result ->
            Logger.i(TAG, "获取token startAuthentication code=$code,result=$result")
            if (2000 != code) {
                promptGenerating.value = false to ""
                "登录失败".showToast()
                return@startAuthentication
            }
            val flashTestTokenInfo = runCatching {
                Gson().fromJson(result, FlashTestTokenInfo::class.java)
            }.getOrNull()

            if (flashTestTokenInfo == null) {
                promptGenerating.value = false to ""
                "登录失败".showToast()
                return@startAuthentication
            }
            loginByFlashMobile(flashValidateToken = flashTestTokenInfo?.token)
        })
    }


    /**
     * 闪验登录
     * @param flashValidateToken 闪验token
     */
    fun loginByFlashMobile(flashValidateToken: String?) {
        getInstall {
            launch(block = {
                response(
                    mRepo.loginByFlashMobile(flashValidateToken),
                    result = {
                        promptGenerating.value = false to ""
                        User.get().updateFrom(data)
                        SignUtils.signIn()
                        data.loginToast()
                        loginResult.value = true
                    },
                    error = {
                        promptGenerating.value = false to ""
                        this?.msg?.showToast()
                    })
            }, onError = {
                promptGenerating.value = false to ""
                "登录失败".showToast()
            })
        }
    }

    /**
     * 登录
     * @param loginType 3-微信登录 4-qq登录 5-苹果登录
     * @param map 第三方授权信息
     */
    fun loginByThird(
        loginType: Int,
        map: MutableMap<String, String>?,
        onLogin: ((UserHomeData?) -> Unit)?
    ) {
        setLoading(true)
        getInstall {
            launchWithLoading(false, block = {
                val body = LoginByThirdBody(
                    loginType = loginType,
                    thirdPartyUnionId = map?.get("unionid"),
                    thirdPartyOpenId = map?.get("openid"),
                    thirdPartyNickName = map?.get("name"),
                    thirdPartyAvatar = map?.get("iconurl"),
                    inviteData = Config.get().installData
                )
                response(mRepo.loginByThird(body), result = {
                    User.get().updateFrom(data)
                    SignUtils.signIn()
                    onLogin?.invoke(data)
                }, error = { this?.msg?.showToast() })
            }, onError = { "登录失败".showToast() })
        }
    }


    /**
     * 发送验证码
     */
    fun sendSmsCode() {
        val mobile = this.mobile.value
        if (!RegexUtils.isMobileSimple(mobile)) {
            T.showShort(R.string.enter_right_phone_num)
            return
        }
        sendCodeEnable.set(false)
        launchWithLoading(
            block = {
                response(
                    mRepo.sendSmsValidateCode(
                        phoneNumber = mobile,
                        bizType = if (isBind.get()) {
                            SendSmsValidateCodeBody.BIZ_TYPE_CHANGE_PHONE
                        } else {
                            SendSmsValidateCodeBody.BIZ_TYPE_LOGIN
                        }
                    ),
                    result = {
                        T.showShort(R.string.get_validate_code_success)
                        startCountdown()
                    },
                    error = {
                        this?.msg?.showToast()
                        sendCodeEnable.set(true)
                    })
            },
            onError = {
                parseMessage(it)?.showToast()
                sendCodeEnable.set(true)
            })
    }

    /**
     * 开始搜索卡倒计时
     */
    private fun startCountdown() {
        val remainSeconds = 60
        updateCountdown(remainSeconds)
        countdownId = System.nanoTime()
        launch {
            val id = countdownId
            flow {
                for (time in remainSeconds downTo 0) {
                    delay(1000)
                    if (id != countdownId) break
                    emit(time)
                }
            }.collect { updateCountdown(it) }
        }
    }


    /**
     * 更新倒计时UI
     */
    private fun updateCountdown(seconds: Int) {
        if (seconds <= 0) {
            stopCountdown()
            return
        }
        sendCodeText.set("${seconds}s")
    }

    /**
     * 停止倒计时，隐藏搜索卡
     */
    private fun stopCountdown() {
        countdownId = 0
        sendCodeText.set(R.string.resend_sms_code.getResString())
        sendCodeEnable.set(true)
    }


    /**
     * 登录
     */
    fun phoneLogin(onLogin: ((UserHomeData?) -> Unit)?) {
        if (!isBind.get() && !checkedPolicy.get()) {
            T.showShort(R.string.please_agree_protocol)
            return
        }
        val mobile = this.mobile.value
        if (!RegexUtils.isMobileSimple(mobile)) {
            T.showShort(R.string.enter_right_phone_num)
            return
        }
        val code = this.code.value
        if (code?.length != Constants.SMS_CODE_LENGTH) {
            T.showShort(R.string.enter_right_validate_code)
            return
        }
        KeyboardUtils.hideSoftInput(ActivityUtils.getTopActivity())
        setLoading(true)
        getInstall {
            launchWithLoading(false, block = {
                if (isBind.get()) {
                    response(mRepo.bindPhone(mobile, code), {
                        "绑定成功".showToast()
                        User.get().apply {
                            storePhoneNum(mobile)
                        }
                        onLogin?.invoke(null)
                    }, error = {
                        this?.msg?.showToast()
                        setLoading(false)
                    })
                } else {
                    response(mRepo.loginByPhone(mobile, code), {
                        User.get().updateFrom(data)
                        SignUtils.signIn()
                        data.loginToast()
                        onLogin?.invoke(data)
                    }, error = {
                        this?.msg?.showToast()
                        setLoading(false)
                    })
                }
            }, onError = {
                setLoading(false)
                parseMessage(it)?.showToast()
            })
        }
    }

}