package com.kgmedia.fastfilm.feature.splash.newer

import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.ExampleWorkInfo

class GuideStep2ViewModel : AppRepositoryViewModel<AppRepository>() {

    val examples = MutableLiveData<List<ExampleWorkInfo>>()

    override fun onCreate() {
        super.onCreate()
        getExamples()
    }

    fun getExamples() {
        requestWithLoading({ mRepo.api.getNewerExampleWorkList() }) {
            examples.value = data
        }
    }
}