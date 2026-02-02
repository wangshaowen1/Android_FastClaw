package com.kgmedia.fastfilm.feature.creator.generate

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.EmptyViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.databinding.DialogConfirmCostBinding
import com.kgmedia.fastfilm.feature.payment.points.PointsRechargeActivity
import com.nhsd.basic.BaseBottomPushVMDialogFragment

class ConfirmCostDialog : BaseBottomPushVMDialogFragment<DialogConfirmCostBinding, EmptyViewModel>() {
    val cost by lazy { arguments?.getInt(Constants.Extra.COST) ?: 0 }
    var onConfirm: ((Boolean) -> Unit)? = null

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvPoints.text = cost.toString()
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.confirmButton -> {
                val isBalanceNotEnough = true//User.get().scoreBalance < cost
                EventManager.onAppTrack(
                    AppEvent.JG_CLICK_GENERATE_IMAGE_POPUP_CONFIRM_BUTTON,
                    GrowingIO.KEY_ALL_COUNT_INT to cost.toString(),
                    GrowingIO.KEY_ALL_IS_CONSUME to if (!isBalanceNotEnough) "yes" else "no"
                )
                if (isBalanceNotEnough) {
                    PointsRechargeActivity.start(mActivity, true)
                    return
                }
                onConfirm?.invoke(true)
                dismissAllowingStateLoss()
            }
        }
    }

    companion object {
        const val TAG = "ConfirmCostDialog"
        fun show(
            fragmentManager: FragmentManager,
            cost: Int,
            onConfirm: ((Boolean) -> Unit)? = null
        ) {
            val dialog = ConfirmCostDialog()
            dialog.arguments = Bundle().apply {
                putInt(Constants.Extra.COST, cost)
            }
            dialog.onConfirm = onConfirm
            dialog.show(fragmentManager)
        }
    }


}