package com.kgmedia.fastfilm.feature.splash.newer

import android.view.Gravity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ScreenUtils
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.getScreenWidth
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.onUserSignIn
import com.kgmedia.fastfilm.core.network.apis.response.FirstRechargeDialogData
import com.kgmedia.fastfilm.databinding.DialogFirstRechargeBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.bus.observeEvents


class FirstRechargeDialog : BaseVMDialogFragment<DialogFirstRechargeBinding, FirstRechargeViewModel>() {


    companion object {

        const val SCENE_HOME = 0
        const val SCENE_OTHER = 1

        fun show(scene: Int = SCENE_OTHER, firstRechargeDialogData: FirstRechargeDialogData?): FirstRechargeDialog? {
            val activity = ActivityUtils.getTopActivity() as? FragmentActivity ?: return null
            val dialog = FirstRechargeDialog().apply {
                arguments = bundleOf(Constants.Extra.DATA to firstRechargeDialogData, Constants.Extra.FROM_PAGE to scene)
            }
            dialog.show(activity.supportFragmentManager)
            return dialog
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = ScreenUtils.getScreenWidth()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.bg.minimumHeight = getScreenWidth()

        observeEvents {
            onUserSignIn {
                if (User.get().hasPaid()) {
                    dismissAllowingStateLoss()
                }
            }
        }
    }
}