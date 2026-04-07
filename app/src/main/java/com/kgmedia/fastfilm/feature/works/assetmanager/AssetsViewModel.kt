package com.kgmedia.fastfilm.feature.works.assetmanager

import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository

class AssetsViewModel : AppRepositoryViewModel<AppRepository>() {

    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = ""
    )

    override fun onCreate() {
        super.onCreate()
        mToolbarVM.mTitle.set(mBundle.getString(Constants.Extra.TITLE))
    }

    val roleCount = MutableLiveData(0)
    val sceneCount = MutableLiveData(0)
    val propCount = MutableLiveData(0)
}