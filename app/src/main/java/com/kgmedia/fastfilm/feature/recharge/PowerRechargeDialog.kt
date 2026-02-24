package com.kgmedia.fastfilm.feature.recharge

import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.databinding.DialogRechargePowerBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents

class PowerRechargeDialog : BaseBottomPushVMDialogFragment<DialogRechargePowerBinding, PowerRechargeViewModel>() {
    override fun onPrepareView() {
        super.onPrepareView()
        observeEvents {
            on<Events.UserDataEvent> {
                mRealVM.updateBalance()
            }
        }
    }
}