package com.kgmedia.fastfilm.feature.settings.about

import android.graphics.Color
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.Config
import com.nhsd.basic.Session
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:关于我们
 * @Data: 2025/4/18  16:55
 * @Version: 1.0
 * *****************************************
 */
class AboutUsViewModel : AppRepositoryViewModel<AppRepository>() {
    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.app_about_us.getResString(),
        titleColor = R.color.textColorPrimary.getResColor(),
        background = Color.TRANSPARENT,
    )

    val appVersion: String by lazy { Session.getInstance().versionName }
    val beiAn by lazy { Config.get().appConfig?.beianNo }
}