package com.kgmedia.fastfilm.feature.payment.points.details

import android.graphics.Color
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:余额明细
 * @Data: 2025/9/10/周三  17:50
 * @Version: 1.0
 * *****************************************
 */
class PointsDetailViewModel : AppRepositoryViewModel<AppRepository>() {
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "余额明细",
        background = Color.TRANSPARENT
    )
}