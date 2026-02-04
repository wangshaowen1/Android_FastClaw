package com.kgmedia.fastfilm.widget

import android.view.LayoutInflater
import android.view.View
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.databinding.PopupSeriesMoreBinding

/**
 * 剧集更多
 */
class SeriesMorePopupView : IPopupView {
    val binding by lazy { PopupSeriesMoreBinding.inflate(LayoutInflater.from(App.instance)) }

    override val root: View
        get() = binding.root
    override val bg: View
        get() = binding.bg
    override val popup: View
        get() = binding.popup
}