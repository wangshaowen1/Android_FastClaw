package com.kgmedia.fastfilm.feature.series.more

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.DialogSeriesMoreBinding
import com.kgmedia.fastfilm.feature.series.editinfo.EditInfoActivity
import com.kgmedia.fastfilm.feature.series.setprice.SetPriceDialog
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 剧集-更多操作
 */
class SeriesMoreDialog : BaseBottomPushVMDialogFragment<DialogSeriesMoreBinding, SeriesMoreViewModel>() {
    companion object {
        const val EXTRA_RELEASED = "released"
        fun show(fragmentManager: FragmentManager?, seriesId: String?, released: Boolean) {
            val fragmentManager = fragmentManager ?: (topActivity as? FragmentActivity)?.supportFragmentManager ?: return
            SeriesMoreDialog().apply {
                arguments = Bundle().apply {
                    putString(Constants.Extra.ID, seriesId)
                    putBoolean(EXTRA_RELEASED, released)
                }
            }.show(fragmentManager)
        }
    }

    private val seriesId by lazy { arguments?.getString(Constants.Extra.ID) }
    private val released by lazy { arguments?.getBoolean(EXTRA_RELEASED) ?: false }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnEdit -> {
                EditInfoActivity.start(seriesId)
                dismissAllowingStateLoss()
            }

            R.id.btnSetPrice -> {
                SetPriceDialog.show(null, seriesId, !released)
                dismissAllowingStateLoss()
            }
        }
    }
}