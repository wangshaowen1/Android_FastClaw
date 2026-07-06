package com.shiyuan.fastclaw.feature.settings

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.nhsd.basic.Session
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.BindThirdPartyBody
import com.shiyuan.fastclaw.core.network.apis.request.UnbindThirdPartyBody
import com.shiyuan.fastclaw.core.network.apis.response.BindThirdPartyData
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.kit.SignUtils

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:设置
 * @Data: 2025/4/18  16:04
 * @Version: 1.0
 * *****************************************
 */
class SettingsViewModel : AppRepositoryViewModel<AppRepository>() {
    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.title_settings.getResString(),
        background = Color.TRANSPARENT,
    )

    val mToolbarVM2 = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.about.getResString(),
        background = Color.TRANSPARENT,
    )

    val mToolbarVM3 = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.record_filing_information.getResString(),
        background = Color.TRANSPARENT,
    )

    val accountSettingsToolbar = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.account_settings.getResString(),
        background = Color.TRANSPARENT,
    )

    val appVersion: String by lazy { "快龙虾Version：" + Session.getInstance().versionName }

    val beiAn by lazy { Config.get().appConfig.beianNo }

    val phone = ObservableField(maskPhone(User.get().phoneNum))

    /** 微信绑定状态 */
    val isWechatBound = ObservableBoolean(false)

    /** 微信绑定展示名 */
    val wechatName = ObservableField("")


    override fun onCreate() {
        super.onCreate()
    }

    fun refreshUserInfo() {
        phone.set(maskPhone(User.get().phoneNum))
        val bound = User.get().wechatBindStatus == User.WECHAT_BIND_STATUS_BOUND
        wechatName.set(if (bound) User.get().wechatNickName else R.string.unbound.getResString())
        isWechatBound.set(bound)
    }

    /**
     * 组装微信第三方绑定参数。
     */
    private fun createWechatBindBody(authData: MutableMap<String, String>?): BindThirdPartyBody {
        return BindThirdPartyBody(
            providerType = BindThirdPartyBody.PROVIDER_TYPE_WECHAT,
            thirdPartyOpenId = authData?.get("openid"),
            thirdPartyUnionId = authData?.get("unionid"),
            thirdPartyNickName = authData?.get("name"),
            thirdPartyAvatar = authData?.get("iconurl"),
            authCode = authData?.get("authCode") ?: authData?.get("code"),
            identityToken = authData?.get("identityToken"),
            nonce = authData?.get("nonce")
        )
    }

    /**
     * 绑定微信账号，接口成功后再更新本地展示状态。
     */
    fun bindWechat(authData: MutableMap<String, String>?, onResult: ((MutableMap<String, String>?, BindThirdPartyData?) -> Unit)? = null) {
        val nickName = authData?.get("name")
        requestWithLoading(
            apiBlock = {
                mRepo.api.bindThirdParty(createWechatBindBody(authData))
            }
        ) {
            if (data?.isSuccess() == true) {
                updateWechatBound(nickName)
                refreshUserInfo()
            }
            onResult?.invoke(authData, data)
        }
    }

    /**
     * 解绑旧账号并绑定到当前账号。
     */
    fun rebindWechat(authData: MutableMap<String, String>?, onResult: ((BindThirdPartyData?) -> Unit)? = null) {
        val nickName = authData?.get("name")
        requestWithLoading(
            apiBlock = {
                mRepo.api.rebindThirdParty(createWechatBindBody(authData))
            }
        ) {
            if (data?.isSuccess() == true) {
                updateWechatBound(nickName)
                refreshUserInfo()
            }
            onResult?.invoke(data)
        }
    }

    /**
     * 解除当前账号的微信绑定。
     */
    fun unbindWechat(onSuccess: (() -> Unit)? = null) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.unbindThirdParty(UnbindThirdPartyBody(UnbindThirdPartyBody.PROVIDER_TYPE_WECHAT))
            }
        ) {
            clearWechatBound()
            refreshUserInfo()
            onSuccess?.invoke()
        }
    }

    /**
     * 退出登录
     */
    fun logoutApp() {
       /* SignUtils.signOut()
        //AppUtils.relaunchApp()
        finish()*/
        // launch { block ={ response(mRepo.api.signOut()) },onError ={},onComplete ={}}
        launch({
            response(mRepo.api.signOut(), result = {}, error = {})
        }, onError = {},
            onComplete = {
                SignUtils.signOut()
                //AppUtils.relaunchApp()
                finish()
            })
    }

    private fun maskPhone(phone: String?): String {
        if (phone.isNullOrBlank()) return R.string.unbound.getResString()
        return if (phone.length >= 7) {
            "${phone.take(3)}*****${phone.takeLast(3)}"
        } else {
            phone
        }
    }

    /**
     * 更新微信绑定展示状态。
     */
    fun updateWechatBound(name: String?) {
        User.get().storeWechatBindStatus(User.WECHAT_BIND_STATUS_BOUND)
        User.get().storeWechatNickName(name?.takeIf { it.isNotBlank() } ?: R.string.wechat_binding.getResString())
        refreshUserInfo()
    }

    /**
     * 清空当前页面的微信绑定展示状态。
     */
    fun clearWechatBound() {
        User.get().storeWechatBindStatus(User.WECHAT_BIND_STATUS_UNBOUND)
        User.get().storeWechatNickName(null)
        refreshUserInfo()
    }
}
