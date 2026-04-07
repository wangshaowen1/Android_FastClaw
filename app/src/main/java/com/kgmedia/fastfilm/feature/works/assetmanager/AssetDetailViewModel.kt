package com.kgmedia.fastfilm.feature.works.assetmanager

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.StringUtils.getString
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.CenterImageSpan
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.ext.PollingState
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchPolling
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.StrategyTypeBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.assets.AssetManagementRepository
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.Job

class AssetDetailViewModel : AppRepositoryViewModel<AssetManagementRepository>() {

    val hasChanged = MutableLiveData(false)
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "",
        menuAction = {
            if (!generating.get() && hasChanged.value == true) {
                topActivity?.showCommonDialog(
                    tip = getString(R.string.confirm_overwrite_asset_warning),
                    confirmText = getString(R.string.save_and_overwrite),
                    confirm = {
                        save()
                    })
            }
        },
        menuEnable = hasChanged.value
    )

    val info by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, SimpleAsset::class.java) }
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val storyId by lazy { mBundle.getString(Constants.Extra.STORY_ID) }
    val maxLength = 200

    val prompt = ObservableField<String>()
    val image = ObservableField<String>()
    val subTitle = ObservableField<String>()

    val generating by lazy { ObservableBoolean(false) }
    val cost by lazy { ObservableInt() }

    /**
     * -1=新创建 0=待生成 1=生成中 2=成功 3=失败
     */
    val status by lazy { ObservableInt() }


    var scriptPollingJob: Job? = null
    private val powerSpan by lazy { CenterImageSpan(App.instance, R.drawable.icon_power).apply { setDrawableSize(16.dp, 16.dp) } }

    override fun onCreate() {
        super.onCreate()
        info?.let {
            prompt.set(it.assetDesc)
            image.set(it.assetImage)
            status.set(it.status ?: 0)

            generating.set(it.status == 1)
            subTitle.set(
                when (info) {
                    is SimpleRoleInfo -> getString(R.string.role_desc)
                    is SimpleSceneInfo -> getString(R.string.scene_desc)
                    else -> getString(R.string.prop_desc)
                }
            )

            getPrice()
        }
    }

    fun getPrice() {
        launch {
            response(mRepo.api.queryAiPrice(StrategyTypeBody(1))) {
                cost.set(data?.pointNum ?: 0)
            }
        }
    }

    fun reGenerate() {
        launchWithLoading {
            response(mRepo.api.workAssetSingleGenerate(GenerateAssetBody())) {
                hasChanged.value = true
                generating.set(true)
                loopGenStatus(data?.taskId)
            }
        }
    }

    fun loopGenStatus(taskId: String?) {
        if (scriptPollingJob?.isActive == true) return

        scriptPollingJob = launchPolling(
            intervalMs = 3000L,
            retryOnNetworkError = true,
            apiBlock = {
                mRepo.api.workAssetSingleStatus(
                    TaskIdBody(
                        taskId, when (info) {
                            is SimpleRoleInfo -> 41
                            is SimpleSceneInfo -> 61
                            else -> 51
                        }
                    )
                )
            },
            checkState = { response ->
                //[0=待生成 1=生成中 2=成功 3=失败]
                when (response?.data?.genStatus) {
                    2 -> PollingState.SUCCESS
                    3 -> PollingState.FAIL
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = { response ->
                generating.set(false)
                image.set(response?.data?.result)
            },
            onFail = { response, _ ->
                generating.set(false)
            }
        )
    }

    fun save(callback: (() -> Unit)? = null) {
        launch {
            response(
                when (info) {
                    is SimpleRoleInfo -> {
                        mRepo.workAssetRoleSave(
                            info?.assetId,
                            info?.assetAppearanceId,
                            workId,
                            storyId,
                            info?.assetName,
                            prompt.get()
                        )
                    }

                    is SimpleSceneInfo -> {
                        mRepo.workAssetSceneSave(
                            info?.assetId,
                            info?.assetAppearanceId,
                            workId,
                            storyId,
                            info?.assetName,
                            prompt.get()
                        )
                    }

                    else -> {
                        mRepo.workAssetPropSave(
                            info?.assetId,
                            info?.assetAppearanceId,
                            workId,
                            storyId,
                            info?.assetName,
                            prompt.get()
                        )
                    }
                }
            ) {
                if (callback != null) {
                    callback()
                    return@response
                }
                back()
            }
        }
    }

    fun buttonText(generating: Boolean, cost: Int): CharSequence? {
        return if (generating) {
            R.string.btn_image_generating.getResString()
        } else {
            if (cost > 0) {
                R.string.btn_regenerate_cost.getResString(POWER, cost)
                    .replaceSpan(POWER) { powerSpan }
            } else {
                R.string.regenerate.getResString()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scriptPollingJob?.cancel()
    }

    companion object {
        private const val POWER = "[power]"
    }
}