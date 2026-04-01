package com.kgmedia.fastfilm.feature.works.assetmanager

import androidx.databinding.ObservableInt
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.CenterImageSpan
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString

class AssetDetailViewModel : AppRepositoryViewModel<AppRepository>() {

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = ""
    )
    val step by lazy { ObservableInt(STEP_INIT) }
    val cost by lazy { ObservableInt() }

    /**
     * -1=新创建 0=待生成 1=生成中 2=成功 3=失败
     */
    val status by lazy { ObservableInt() }
    private val powerSpan by lazy { CenterImageSpan(App.instance, R.drawable.icon_power).apply { setDrawableSize(16.dp, 16.dp) } }

    fun buttonEnable(step: Int): Boolean {
        return step == STEP_DESCRIPTION_GENERATED || step == STEP_IMAGE_GENERATE_FAILED || step == STEP_IMAGE_GENERATED
    }
    fun buttonText(step: Int, cost: Int): CharSequence? {
        return when (step) {
            STEP_DESCRIPTION_GENERATED -> {
                if (cost > 0) {
                    R.string.btn_one_click_generate_cost.getResString(POWER, cost)
                        .replaceSpan(POWER) { powerSpan }
                } else {
                    R.string.btn_one_click_generate.getResString()
                }
            }

            STEP_IMAGE_GENERATING -> {
                R.string.btn_image_generating.getResString()
            }

            STEP_IMAGE_GENERATE_FAILED -> {
                if (cost > 0) {
                    R.string.btn_retry_failed_cost.getResString(POWER, cost)
                        .replaceSpan(POWER) { powerSpan }
                } else {
                    R.string.btn_retry_failed.getResString()
                }
            }

            STEP_IMAGE_GENERATED -> {
                R.string.btn_next_step_storyboard.getResString()
            }

            else -> null
        }
    }

    companion object {
        private const val POWER = "[power]"

        /**
         * 初始化
         */
        private const val STEP_INIT = 0

        /**
         * 资产描述生成中
         */
        private const val STEP_DESCRIPTION_GENERATING = 1

        /**
         * 资产描述已生成
         */
        private const val STEP_DESCRIPTION_GENERATED = 2

        /**
         * 资产图片生成中
         */
        const val STEP_IMAGE_GENERATING = 3

        /**
         * 资产图片生成失败
         */
        private const val STEP_IMAGE_GENERATE_FAILED = 4

        /**
         * 资产图片已生成
         */
        private const val STEP_IMAGE_GENERATED = 5
    }
}