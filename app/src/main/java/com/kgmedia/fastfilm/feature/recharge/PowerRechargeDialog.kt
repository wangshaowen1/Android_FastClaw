package com.kgmedia.fastfilm.feature.recharge

import androidx.fragment.app.FragmentActivity
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.databinding.DialogRechargePowerBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.showToast

class PowerRechargeDialog : BaseBottomPushVMDialogFragment<DialogRechargePowerBinding, PowerRechargeViewModel>() {
    companion object {
        /**
         * 检查是否余额不足，并弹出充值弹窗
         */
        fun checkLowBalance(cost: Int): Boolean {
            if (cost > User.get().computingPower) {
                show(true)
                return true
            }
            return false
        }

        fun show(showToast: Boolean = false) {
            (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
                if (it.fragments.any { fragment -> fragment is PowerRechargeDialog }) return
                PowerRechargeDialog().show(it)
                if (showToast) {
                    "算力余额不足，请先充值算力".showToast()
                }
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        observeEvents {
            on<Events.UserDataEvent> {
                mRealVM.updateBalance()
            }
        }
    }
}