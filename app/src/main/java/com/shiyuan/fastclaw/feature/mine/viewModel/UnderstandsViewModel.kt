package com.shiyuan.fastclaw.feature.mine.viewModel

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的
 * @Data: 2025/4/18  14:10
 * @Version: 1.0
 * *****************************************
 */
class UnderstandsViewModel : AppRepositoryViewModel<AppRepository>() {


    val mToolbarVMUnderstands = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.shrimp_understands.getResString(),
        background = Color.TRANSPARENT,

        )

    var tableIndex = MutableLiveData(0)

}