package com.kgmedia.fastfilm.feature.recharge

import androidx.fragment.app.FragmentActivity
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.databinding.DialogRechargePowerBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents

class PowerRechargeDialog : BaseBottomPushVMDialogFragment<DialogRechargePowerBinding, PowerRechargeViewModel>() {
    companion object {
        fun show() {
            (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
                PowerRechargeDialog().show(it)
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