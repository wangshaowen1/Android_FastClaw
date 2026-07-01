package com.shiyuan.fastclaw.feature.settings

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.blankj.utilcode.util.AppUtils
import com.nhsd.basic.Session
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.response
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.network.apis.request.ResumeMaterialQualityBody
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.FAILED
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

    val maskedPhone: String
        get() = maskPhone(User.get().phoneNum)

    /** 微信绑定状态 */
    val isWechatBound = ObservableBoolean(false)

    /** 微信绑定展示名 */
    val wechatName = ObservableField(R.string.unbound.getResString())


    override fun onCreate() {
        super.onCreate()

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
        isWechatBound.set(true)
        wechatName.set(name?.takeIf { it.isNotBlank() } ?: R.string.wechat_binding.getResString())
    }

    /**
     * 清空当前页面的微信绑定展示状态。
     */
    fun clearWechatBound() {
        isWechatBound.set(false)
        wechatName.set(R.string.unbound.getResString())
    }

}
