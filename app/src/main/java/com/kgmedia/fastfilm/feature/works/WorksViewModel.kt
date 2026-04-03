package com.kgmedia.fastfilm.feature.works

import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository

class WorksViewModel : AppRepositoryViewModel<AppRepository>() {

    var editModel = MutableLiveData(false)
}