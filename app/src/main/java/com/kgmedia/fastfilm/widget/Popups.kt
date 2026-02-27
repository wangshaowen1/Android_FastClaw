package com.kgmedia.fastfilm.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.kgmedia.fastfilm.databinding.PopupSeriesMoreBinding
import com.kgmedia.fastfilm.databinding.PopupSeriesReleaseFailTipsBinding

/**
 * 剧集更多
 */
class SeriesMorePopupView(context: Context) : IPopupView {
    val binding by lazy { PopupSeriesMoreBinding.inflate(LayoutInflater.from(context), null, false) }

    override val root: View
        get() = binding.root
    override val bg: View
        get() = binding.bg
    override val popup: View
        get() = binding.popup
}


/**
 * 剧集签约失败提示
 */
class SeriesReleaseFailTipsPopupView(context: Context) : IPopupView {
    val binding by lazy { PopupSeriesReleaseFailTipsBinding.inflate(LayoutInflater.from(context), null, false) }

    override val root: View
        get() = binding.root
    override val bg: View
        get() = binding.bg
    override val popup: View
        get() = binding.popup

    override fun updateView(animatedValue: Float): Boolean {
        //默认动画
        bg.alpha = animatedValue
        popup.pivotX = 0.3736263f * popup.width
        popup.pivotY = 0f
        popup.scaleX = animatedValue
        popup.scaleY = animatedValue
        return true
    }
}