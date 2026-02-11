package com.kgmedia.fastfilm.feature.login.unified

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.getInstall
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.loginToast
import com.kgmedia.fastfilm.core.network.apis.request.LoginByThirdBody
import com.kgmedia.fastfilm.core.network.apis.response.UserHomeData
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.login.data.ThirdLoginRepository
import com.kgmedia.fastfilm.kit.SignUtils
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:聚合登录
 * @Data: 2026/2/11/周三  11:37
 * @Version: 1.0
 * *****************************************
 */
class UnifiedLoginViewModel : AppRepositoryViewModel<ThirdLoginRepository>() {

    /**
     * 协议是否勾选
     */
    val checkedPolicy = ObservableBoolean(false)


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
                    data.loginToast()
                    onLogin?.invoke(data)
                }, error = { this?.msg?.showToast() })
            }, onError = { "登录失败".showToast() })
        }
    }
}