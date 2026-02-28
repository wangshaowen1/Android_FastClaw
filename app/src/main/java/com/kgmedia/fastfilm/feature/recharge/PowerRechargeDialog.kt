package com.kgmedia.fastfilm.feature.recharge

import androidx.fragment.app.FragmentActivity
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.setupCommonBanner
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.core.network.apis.request.BannerBody
import com.kgmedia.fastfilm.databinding.DialogRechargePowerBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.showToast
import com.zhpan.bannerview.BannerViewPager

class PowerRechargeDialog : BaseBottomPushVMDialogFragment<DialogRechargePowerBinding, MyBalanceViewModel>() {
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
                    "算力余额不足，请及时充值".showToast()
                }
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        setupCommonBanner(binding.banner.root, binding.banner.banner as BannerViewPager<BannerInfo>) {
            ApiFactory.apiService.getAppBanner(BannerBody(BannerBody.LOCATION_PAY_POPUP)).data
        }
        observeEvents {
            on<Events.UserDataEvent> {
                mRealVM.updateBalance()
            }
        }
    }

    override fun createViewModel(): MyBalanceViewModel {
        return MyBalanceViewModel(R.layout.item_recharge_payment_dialog)
    }
}