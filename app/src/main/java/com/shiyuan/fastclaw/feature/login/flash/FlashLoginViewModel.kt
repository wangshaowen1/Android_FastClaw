package com.shiyuan.fastclaw.feature.login.flash


import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.chuanglan.shanyan_sdk.OneKeyLoginManager
import com.google.gson.Gson
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.loginToast
import com.shiyuan.fastclaw.core.manager.FlashLoginManager
import com.shiyuan.fastclaw.core.network.apis.entity.FlashTestPreFetchNumberInfo
import com.shiyuan.fastclaw.core.network.apis.entity.FlashTestTokenInfo
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.network.apis.response.UserHomeData
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.login.data.FlashLoginRepository
import com.shiyuan.fastclaw.kit.SignUtils
import com.nhsd.basic.ext.showToast

/**
 * 闪验登录
 * @author xiaoowuu
 */
class FlashLoginViewModel : AppRepositoryViewModel<FlashLoginRepository>() {

    /** 是否已勾选登录协议。 */
    val checkedPolicy = ObservableBoolean(false)

    /** 闪验预取号返回的手机号与运营商协议信息。 */
    val flashPhoneInfo = ObservableField<FlashTestPreFetchNumberInfo?>()

    /** 底部弹框展示的脱敏手机号。 */
    val flashPhoneNumber = ObservableField("")

    /** 闪验预取号失败事件，通知页面切换到手机号登录。 */
    val flashPreFetchFailed = MutableLiveData(false)

    /** 闪验登录成功事件，通知页面关闭弹框。 */
    val flashLoginResult = MutableLiveData(false)

    /**
     * 初始化协议勾选状态并预取本机号码。
     */
    fun initFlashLogin(checkedPolicy: Boolean) {
        this.checkedPolicy.set(checkedPolicy)
        preFetchPhoneNumber()
    }

    /**
     * 通过闪验 SDK 预取本机号码，拿不到号码时回退手机号登录。
     */
    private fun preFetchPhoneNumber() {
        OneKeyLoginManager.getInstance().getPhoneInfo { code, result ->
            if (code != 1022) {
                flashPreFetchFailed.value = true
                return@getPhoneInfo
            }
            val info = runCatching {
                Gson().fromJson(result, FlashTestPreFetchNumberInfo::class.java)
            }.getOrNull()
            if (info?.number.isNullOrBlank()) {
                flashPreFetchFailed.value = true
                return@getPhoneInfo
            }
            flashPhoneInfo.set(info)
            flashPhoneNumber.set(info?.number.orEmpty())
        }
    }

    /**
     * 通过闪验 SDK 获取 token，并用 token 调用本机号码一键登录接口。
     */
    fun flashLoginVerification() {
        OneKeyLoginManager.getInstance().startAuthentication { code, result ->
            if (code != 2000) {
                showToast(R.string.toast_sign_in_fail)
                return@startAuthentication
            }
            val tokenInfo = runCatching {
                Gson().fromJson(result, FlashTestTokenInfo::class.java)
            }.getOrNull()
            if (tokenInfo?.token.isNullOrBlank()) {
                showToast(R.string.toast_sign_in_fail)
                return@startAuthentication
            }
            loginByFlashMobile(tokenInfo?.token) {
                flashLoginResult.value = true
            }
        }
    }

    /**
     * 闪验登录
     * @param flashValidateToken 闪验token
     */
    fun loginByFlashMobile(flashValidateToken: String?, onLogin: ((UserHomeData?) -> Unit)?) {
        launch(block = {
            response(
                mRepo.loginByFlashMobile(flashValidateToken),
                result = {
                    User.get().updateFrom(data)
                    SignUtils.signIn()
                    data.loginToast()
                    onLogin?.invoke(data)
                    dismiss()
                },
                error = {
                    this?.msg?.showToast()
                    FlashLoginManager.hideLoading()
                })
        }, onError = {
            showToast(R.string.toast_sign_in_fail)
            FlashLoginManager.hideLoading()
        })
    }

    /**
     * 登录
     * @param loginType 3-微信登录 4-qq登录 5-苹果登录
     * @param map 第三方授权信息
     */
    fun loginByThird(loginType: Int, map: MutableMap<String, String>?, onLogin: ((UserHomeData?) -> Unit)?) {
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
                onLogin?.invoke(data)
            }, error = { this?.msg?.showToast() })
        }, onError = { showToast(R.string.toast_sign_in_fail) })
    }
}
