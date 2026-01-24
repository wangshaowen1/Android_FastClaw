package com.kgmedia.fastfilm.feature.login

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.request.CompleteUserDataInfoBody
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.network.apis.response.AgeRange
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.widget.LoadStateViewHolder

/**
 * 选择性别和年龄
 */
class SimpleProfileViewModel : AppRepositoryViewModel<AppRepository>() {
    val ageAdapter by lazy { QuickAdapter(R.layout.item_age_range, emptyList<ItemAgeRangeVM>()) }
    val selectedItem = ObservableField<AgeRange>()
    val gender by lazy { ObservableInt() }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.LOADING) }
    fun applyGender(gender: Int) {
        this.gender.set(gender)
    }

    override fun onCreate() {
        super.onCreate()
        val list = Config.get().appConfig.ageRangeList
        if (list.isNullOrEmpty()) {
            loadAgeRange()
        } else {
            setAgeRangeList(list)
        }
    }

    fun loadAgeRange() {
        loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                response(
                    bean = mRepo.api.appConfig(UserTokenBody()),
                    result = {
                        val list = data?.ageRangeList
                        if (list.isNullOrEmpty()) {
                            loadState.set(LoadStateViewHolder.RETRY)
                        } else {
                            setAgeRangeList(list)
                        }
                    },
                    error = {
                        loadState.set(LoadStateViewHolder.RETRY)
                    })
            },
            onError = {
                loadState.set(LoadStateViewHolder.RETRY)
            })
    }

    private fun setAgeRangeList(list: List<AgeRange>) {
        loadState.set(LoadStateViewHolder.CONTENT)
        list.map { item ->
            ItemAgeRangeVM(item, selectedItem, gender).apply {
                onItemClick.value = { selectedItem.set(item) }
            }
        }.let {
            ageAdapter.submitList(it)
        }
    }

    /**
     * 提交
     */
    fun commit() {
        EventManager.onAppTrack(AppEvent.JG_CLICK_COMPLETE_PROFILE_POPUP_CONFIRM_BUTTON)
        launchWithLoading {
            val body = CompleteUserDataInfoBody(gender = gender.get(), userV2EraId = selectedItem.get()?.id)
            response(mRepo.api.modifyUserInfo(body)) {
                User.get().updateFromCompleteUserBody(body)
                dismiss()
            }
        }
    }
}