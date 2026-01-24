package com.kgmedia.fastfilm.feature.hit.recommend

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.LoadState
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.network.apis.entity.HitTabInfo
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.ext.showToast


class HitRecommendViewModel : AppRepositoryViewModel<AppRepository>() {

    val tabs = MutableLiveData<List<HitTabInfo>>()
    val showHelp = ObservableField<LoadState>(LoadState.Loading)

    override fun onCreate() {
        super.onCreate()
        refreshTabs()
    }

    fun refreshTabs() {
        showHelp.set(LoadState.Loading)
        launch({
            response(mRepo.api.hitRecommendTabs(UserTokenBody()), result = {
                tabs.value = data?.items
                showHelp.set(if (data?.items?.isEmpty() == true) LoadState.Empty else LoadState.NotLoading(false))
            }, error = {
                this?.let {
                    showHelp.set(LoadState.Error(apiException))
                    showToastError(this.msg)
                }
            })
        }, {
            showHelp.set(LoadState.Error(it))
            parseMessage(it)?.showToast()
        })
    }
}