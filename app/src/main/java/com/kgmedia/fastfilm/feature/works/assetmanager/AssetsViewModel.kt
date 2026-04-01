package com.kgmedia.fastfilm.feature.works.assetmanager

import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.nhsd.basic.ext.getResString

class AssetsViewModel : AppRepositoryViewModel<AppRepository>() {

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = ""
    )
}