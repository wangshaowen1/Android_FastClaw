package com.kgmedia.fastfilm.feature.play

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.databinding.DialogUnlockShortBinding
import com.kgmedia.fastfilm.feature.payment.points.PointsRechargeActivity
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 解锁剧集
 */
class UnlockSeriesShortDialog :
    BaseBottomPushVMDialogFragment<DialogUnlockShortBinding, UnLockSeriesShortViewModel>() {

    companion object {
        const val LOCKED = "locked"
        const val UNLOCK_COINS = "unlockCoins"

        fun show(
            manager: FragmentManager,
            position: String? = null,
            isLocked: Boolean = false,
            unLockCoins: Int? = null
        ): UnlockSeriesShortDialog {
            val dialog = UnlockSeriesShortDialog()
            dialog.arguments = Bundle().apply {
                position?.let { putString(Constants.Extra.POSITION, it) }
                putBoolean(LOCKED, isLocked)
                unLockCoins?.let {
                    putInt(UNLOCK_COINS, it)
                }
            }
            dialog.show(manager)
            return dialog
        }
    }

    var unlock: (Boolean) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.unlockNow -> {
                val checkBalance = User.get().checkBalance(mRealVM.unlockCoins)
                if (checkBalance) {
                    unlock.invoke(mRealVM.shortLocked.get())
                    dismissAllowingStateLoss()
                } else {
                    PointsRechargeActivity.start(activity, true)
                }
                EventManager.onAppTrack(
                    AppEvent.JG_CLICK_POPUP_UNLOCK_SERIES_UNLOCK_NOW_BUTTON, GrowingIO.ALL_IS_UNLOCKED to if (checkBalance) "yes" else "no",
                    GrowingIO.KEY_ALL_COUNT_INT to mRealVM.unlockCoins.toString(),
                )
            }

            R.id.textView -> {
                UnlockNoticeDialog.show(childFragmentManager)
            }

            R.id.autoUnlockSwitch -> {
                mRealVM.shortLocked.set(mRealVM.shortLocked.get().not())
            }
        }
    }


}