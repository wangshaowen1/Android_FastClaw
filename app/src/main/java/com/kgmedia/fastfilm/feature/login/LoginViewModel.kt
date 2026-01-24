package com.kgmedia.fastfilm.feature.login

import android.graphics.Color
import android.graphics.Typeface
import androidx.databinding.ObservableBoolean
import com.drake.spannable.addSpan
import com.drake.spannable.span.HighlightSpan
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.getInstall
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.loginToast
import com.kgmedia.fastfilm.core.network.apis.request.LoginByThirdBody
import com.kgmedia.fastfilm.core.network.apis.response.UserHomeData
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.login.data.ThirdLoginRepository
import com.kgmedia.fastfilm.feature.settings.SettingsActivity
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kgmedia.fastfilm.kit.SignUtils
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

/**
 * 第三方登录
 */
class LoginViewModel : AppRepositoryViewModel<ThirdLoginRepository>() {

    /**
     * 协议是否勾选
     */
    val checkedPolicy by lazy { ObservableBoolean(false) }
    val policyText by lazy {
        "我已阅读并同意"
            .addSpan("《用户协议》", HighlightSpan(Color.BLACK, Typeface.DEFAULT_BOLD) {
                val activity = topActivity ?: return@HighlightSpan
                SimpleBrowserActivity.launch(activity, Config.get().serviceUrl, R.string.tvSYUserService.getResString())
            })
            .addSpan("、")
            .addSpan("《隐私政策》", HighlightSpan(Color.BLACK, Typeface.DEFAULT_BOLD) {
                val activity = topActivity ?: return@HighlightSpan
                SimpleBrowserActivity.launch(activity, Config.get().privacyUrl, R.string.tvSYPrivateService.getResString())
            })
            //不加这个空格，TextView点击会跳协议
            .addSpan(" ")
    }

    /**
     * 登录
     * @param loginType 3-微信登录 4-qq登录 5-苹果登录
     * @param map 第三方授权信息
     */
    fun loginByThird(loginType: Int, map: MutableMap<String, String>?, onLogin: ((UserHomeData?) -> Unit)?) {
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
                    data.loginToast()
                    onLogin?.invoke(data)
                }, error = { this?.msg?.showToast() })
            }, onError = { "登录失败".showToast() })
        }
    }

    fun checkPolicy(){
        checkedPolicy.set(checkedPolicy.get().not())
    }

    fun debugToSettings(){
        startActivity(SettingsActivity::class.java)
    }
}