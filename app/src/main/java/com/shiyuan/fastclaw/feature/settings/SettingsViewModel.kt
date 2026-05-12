package com.shiyuan.fastclaw.feature.settings

import android.graphics.Color
import androidx.databinding.ObservableField
import com.blankj.utilcode.util.AppUtils
import com.nhsd.basic.Session
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.response
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
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

    val appVersion: String by lazy { "快龙虾版本："+ Session.getInstance().versionName }

    val beiAn by lazy { Config.get().appConfig.beianNo }



    override fun onCreate() {
        super.onCreate()

    }

    /**
     * 退出登录
     */
    fun logoutApp() {
        SignUtils.signOut()
        AppUtils.relaunchApp()
      /*  launch { response(mRepo.api.logoutApp(UserTokenBody())) {

        } }*/
    }

    override fun onResume() {
    }
}