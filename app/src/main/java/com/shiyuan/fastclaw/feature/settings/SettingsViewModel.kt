package com.shiyuan.fastclaw.feature.settings

import android.graphics.Color
import androidx.databinding.ObservableField
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

    val debugIp = ObservableField<String>()

    var phone = ObservableField(User.get().phoneNum)

    val beiAn by lazy { Config.get().appConfig.beianNo }

    /**
     * 退出登录
     */
    fun logoutApp() {
        launch { response(mRepo.api.logoutApp(UserTokenBody())) {} }
    }

    override fun onResume() {
        super.onResume()
        phone.set(User.get().phoneNum)
    }
}