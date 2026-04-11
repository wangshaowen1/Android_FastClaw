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
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.ext.PollingState
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchPolling
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.RoleImageInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleAsset
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleRoleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleSceneInfo
import com.kgmedia.fastfilm.core.network.apis.request.AppearanceIdBody
import com.kgmedia.fastfilm.core.network.apis.request.AssetSaveOverrideBody
import com.kgmedia.fastfilm.core.network.apis.request.GenerateAssetBody
import com.kgmedia.fastfilm.core.network.apis.request.StrategyTypeBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.assets.AssetManagementRepository
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

class AssetDetailViewModel : AppRepositoryViewModel<AssetManagementRepository>() {

    val hasChanged = MutableLiveData(false)
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "",
        menuAction = {
            if (generating.value == false && hasChanged.value == true) {
                topActivity?.showCommonDialog(
                    tip = getString(R.string.confirm_overwrite_asset_warning),
                    confirmText = getString(R.string.save_and_overwrite),
                    confirm = {
                        save()
                    })
            }
        },
        menuButtonText = getString(R.string.save),
        menuButtonBackground = R.drawable.bg_white_rectangle_corner_r50.getDrawable(),
        menuButtonTextColor = R.color.bg_color_1_dark.getResColor(),
        menuEnable = hasChanged.value
    )

    val info by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, SimpleAsset::class.java) }
    val roleImageInfo by lazy { mBundle.getParcelableValue(Constants.Extra.ROLE_IMAGE, RoleImageInfo::class.java) }
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val maxLength = 200

    val prompt = ObservableField<String>()
    val image = ObservableField<String>()
    val subTitle = ObservableField<String>()

    val generating by lazy { MutableLiveData(false) }
    val regenerateFail by lazy { ObservableBoolean(false) }
    val cost by lazy { ObservableInt() }

    /**
     * -1=新创建 0=待生成 1=生成中 2=成功 3=失败
     */
    val status by lazy { ObservableInt() }
    var newAppearanceId: String? = null


    var scriptPollingJob: Job? = null
    private val powerSpan by lazy { CenterImageSpan(App.instance, R.drawable.icon_power).apply { setDrawableSize(16.dp, 16.dp) } }

    override fun onCreate() {
        super.onCreate()
        if (roleImageInfo != null) {
            roleImageInfo?.let {
                prompt.set(it.rolePrompt)
                image.set(it.imageUrl)
                status.set(it.genStatus ?: 0)

                generating.value = it.genStatus == 1
                subTitle.set(getString(R.string.role_desc))
            }
        } else {
            info?.let {
                prompt.set(it.assetDesc)
                image.set(it.assetImage)
                status.set(it.status ?: 0)

                generating.value = it.status == 1
                subTitle.set(
                    when (info) {
                        is SimpleRoleInfo -> getString(R.string.role_desc)
                        is SimpleSceneInfo -> getString(R.string.scene_desc)
                        else -> getString(R.string.prop_desc)
                    }
                )
            }
        }
        getPrice()
    }

    fun getPrice() {
        launch {
            response(mRepo.api.queryAiPrice(StrategyTypeBody(1))) {
                cost.set(data?.pointNum ?: 0)
            }
        }
    }

    fun reGenerate() {
        if (PowerRechargeDialog.checkLowBalance(cost.get())) return
        regenerateFail.set(false)
        launchWithLoading {
            response(
                mRepo.api.workAssetSingleRegenerate(
                    GenerateAssetBody(
                        workId = workId,
                        appearanceId = roleImageInfo?.appearanceId ?: info?.assetAppearanceId,
                        prompt = prompt.get(),
                        bizType = when (info) {
                            is SimpleRoleInfo -> 41
                            is SimpleSceneInfo -> 61
                            else -> 51
                        },
                    )
                )
            ) {
                if (data?.genStatus != 3) {
                    Global.refreshBalance()
                    image.set(null)
                    newAppearanceId = data?.newAppearanceId
                    hasChanged.value = true
                    loopGenStatus(data?.taskId)
                    generating.value = true
                } else {
                    image.set(null)
                    regenerateFail.set(true)
                }
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
                generating.value = false
                image.set(response?.data?.result)
            },
            onFail = { response, _ ->
                generating.value = false
                image.set(null)
                regenerateFail.set(true)
            }
        )
    }

    fun save(callback: (() -> Unit)? = null) {
        launch {
            val bizType = when (info) {
                is SimpleRoleInfo -> 41
                is SimpleSceneInfo -> 61
                else -> 51
            }
            response(mRepo.api.assetSaveOverride(AssetSaveOverrideBody(bizType, newAppearanceId, workId))) {
                hasChanged.value = false
                if (callback != null) {
                    callback()
                    return@response
                }
                back()
            }
        }
    }

    fun buttonText(generating: Boolean, regenerateFail: Boolean, cost: Int): CharSequence? {
        return if (generating) {
            R.string.btn_image_generating.getResString()
        } else {
            if (regenerateFail) {
                return R.string.image_load_failed_regenerate.getResString()
            }
            if (cost > 0) {
                R.string.btn_regenerate_cost.getResString(POWER, cost)
                    .replaceSpan(POWER) { powerSpan }
            } else {
                R.string.regenerate.getResString()
            }
        }
    }

    fun imageDel(callback: (() -> Unit)?) {
        launch {
            val asset = info
            val response = withContext(Dispatchers.IO) {
                when (asset) {
                    is SimpleRoleInfo -> mRepo.api.roleImageDelete(AppearanceIdBody(newAppearanceId))
                    is SimpleSceneInfo -> mRepo.api.sceneImageDelete(AppearanceIdBody(newAppearanceId))
                    else -> mRepo.api.propImageDelete(AppearanceIdBody(newAppearanceId))
                }
            }
            callback?.invoke()
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