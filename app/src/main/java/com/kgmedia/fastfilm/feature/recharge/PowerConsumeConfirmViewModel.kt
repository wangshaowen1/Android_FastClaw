package com.kgmedia.fastfilm.feature.recharge

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository


class PowerConsumeConfirmViewModel : AppRepositoryViewModel<AppRepository>() {
    val scene by lazy { mBundle.getInt(Constants.Extra.TYPE) }
    val cost by lazy { mBundle.getInt(Constants.Extra.COST) }
    val title by lazy {
        when (scene) {
            PowerConsumeConfirmDialog.SCENE_GENERATE_ASSETS_IMAGE -> "生成资产图片"
            PowerConsumeConfirmDialog.SCENE_GENERATE_STORYBOARD_VIDEO -> "生成分镜视频"
            PowerConsumeConfirmDialog.SCENE_REGENERATE_STORYBOARD_VIDEO -> "重绘分镜视频"
            else -> ""
        }
    }

    /**
     * 协议是否勾选
     */
    val checkedPolicy by lazy { ObservableBoolean(true) }

    fun checkPolicy() {
        checkedPolicy.set(checkedPolicy.get().not())
    }
}