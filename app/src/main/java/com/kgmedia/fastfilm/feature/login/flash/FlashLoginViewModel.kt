package com.kgmedia.fastfilm.feature.login.flash


import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.getInstall
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.loginToast
import com.kgmedia.fastfilm.core.manager.FlashLoginManager
import com.kgmedia.fastfilm.core.network.apis.request.LoginByThirdBody
import com.kgmedia.fastfilm.core.network.apis.response.UserHomeData
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.login.data.FlashLoginRepository
import com.kgmedia.fastfilm.kit.SignUtils
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
        getInstall {
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
                    onLogin?.invoke(data)
                }, error = { this?.msg?.showToast() })
            }, onError = { showToast(R.string.toast_sign_in_fail) })
        }
    }
}