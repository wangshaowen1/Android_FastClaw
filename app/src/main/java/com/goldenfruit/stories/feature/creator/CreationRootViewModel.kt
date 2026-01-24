package com.kgmedia.fastfilm.feature.creator

import android.graphics.Color
import androidx.databinding.ObservableFloat
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.feature.aicompose.generating.GeneratingViewModel
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.ext.getResColor

class CreationRootViewModel : AppRepositoryViewModel<CreationRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "创作",
        titleColor = R.color.textColorPrimary.getResColor(),
        background = Color.TRANSPARENT,
    )

    val tabIndicatorPosition by lazy { ObservableFloat(0f) }
    val selectedTab by lazy { MutableLiveData<Int>() }

    override fun onCreate() {
        super.onCreate()
    }

}