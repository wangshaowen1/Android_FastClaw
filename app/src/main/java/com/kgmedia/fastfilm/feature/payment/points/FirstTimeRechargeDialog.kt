package com.kgmedia.fastfilm.feature.payment.points

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.DialogFirstTimeRechargeBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 首次充值弹窗
 */
class FirstTimeRechargeDialog : BaseBottomPushVMDialogFragment<DialogFirstTimeRechargeBinding, FirstTimeRechargeViewModel>() {
    companion object {
        fun show(fragmentManager: FragmentManager?, dramasId: String?, cost: Int): FirstTimeRechargeDialog? {
            val fragmentManager = fragmentManager ?: (topActivity as? FragmentActivity)?.supportFragmentManager ?: return null
            return FirstTimeRechargeDialog().apply {
                arguments = Bundle().apply {
                    putString(Constants.Extra.DRAMAS_ID, dramasId)
                    putInt(Constants.Extra.COST, cost)
                }
                show(fragmentManager)
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onAppTrack(AppEvent.JG_POPUP_UNLOCK_WORK_RECHARGE_UNLOCK)
    }
}