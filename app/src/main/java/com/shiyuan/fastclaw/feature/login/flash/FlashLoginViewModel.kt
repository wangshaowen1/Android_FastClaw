package com.shiyuan.fastclaw.feature.login.flash


import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.loginToast
import com.shiyuan.fastclaw.core.manager.FlashLoginManager
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
                inviteData = Config.get().installData
            )
            response(mRepo.loginByThird(body), result = {
                User.get().updateFrom(data)
                SignUtils.signIn()
                onLogin?.invoke(data)
            }, error = { this?.msg?.showToast() })
        }, onError = { showToast(R.string.toast_sign_in_fail) })
    }
}
