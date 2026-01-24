package com.kgmedia.fastfilm.feature.series.setprice

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.databinding.DialogSeriesSetPriceBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 剧集-设置价格
 */
class SetPriceDialog : BaseBottomPushVMDialogFragment<DialogSeriesSetPriceBinding, SetPriceViewModel>() {
    companion object {
        const val EXTRA_RELEASE = "release"
        fun show(fragmentManager: FragmentManager?, seriesId: String?, release: Boolean) {
            val fragmentManager = fragmentManager ?: (topActivity as? FragmentActivity)?.supportFragmentManager ?: return
            SetPriceDialog().apply {
                arguments = Bundle().apply {
                    putString(Constants.Extra.ID, seriesId)
                    putBoolean(EXTRA_RELEASE, release)
                }
            }.show(fragmentManager)
        }
    }

}