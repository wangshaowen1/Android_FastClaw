package com.shiyuan.fastclaw.feature.login

import android.graphics.Color
import android.graphics.Typeface
import androidx.databinding.ObservableBoolean
import com.drake.spannable.addSpan
import com.drake.spannable.span.HighlightSpan
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.common.ext.topActivity
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.loginToast
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.network.apis.response.UserHomeData
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.login.data.ThirdLoginRepository
import com.shiyuan.fastclaw.feature.settings.SettingsActivity
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.kit.SignUtils
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.getThemeColor
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
        R.string.i_have_read_and_agree_to.getResString()
            .addSpan(R.string.tvUserService.getResString(), HighlightSpan(topActivity?.getThemeColor(R.attr.textColor1), Typeface.DEFAULT_BOLD) {
                val activity = topActivity ?: return@HighlightSpan
                SimpleBrowserActivity.launch(activity, Config.get().serviceUrl, R.string.tvSYUserService.getResString())
            })
            .addSpan("、")
            .addSpan(R.string.tvPrivateService.getResString(), HighlightSpan(topActivity?.getThemeColor(R.attr.textColor1), Typeface.DEFAULT_BOLD) {
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
        }, onError = { showToast(R.string.toast_sign_in_fail) })
    }

    fun checkPolicy(){
        checkedPolicy.set(checkedPolicy.get().not())
    }

    fun debugToSettings(){
        startActivity(SettingsActivity::class.java)
    }
}
