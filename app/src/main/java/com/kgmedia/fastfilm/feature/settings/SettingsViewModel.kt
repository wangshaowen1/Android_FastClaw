package com.kgmedia.fastfilm.feature.settings

import android.graphics.Color
import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response
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

    var phone = ObservableField(User.get().phoneBind)

    val beiAn by lazy { Config.get().appConfig.beianNo }

    /**
     * 退出登录
     */
    fun logoutApp() {
        launch { response(mRepo.api.logoutApp(UserTokenBody())) {} }
    }

    override fun onResume() {
        super.onResume()
        phone.set(User.get().phoneBind)
    }
}