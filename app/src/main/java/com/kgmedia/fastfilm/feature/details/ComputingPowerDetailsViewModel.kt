package com.kgmedia.fastfilm.feature.details

import android.graphics.Color
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.nhsd.basic.ext.getResString

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:算力明细
 * @Data: 2025/9/10/周三  17:50
 * @Version: 1.0
 * *****************************************
 */
class ComputingPowerDetailsViewModel : AppRepositoryViewModel<AppRepository>() {
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.compute_details.getResString(),
        background = Color.TRANSPARENT
    )
}