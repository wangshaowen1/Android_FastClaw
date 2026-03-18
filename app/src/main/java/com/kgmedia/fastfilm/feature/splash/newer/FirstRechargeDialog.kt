package com.kgmedia.fastfilm.feature.splash.newer

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.network.apis.response.FirstRechargeDialogData
import com.kgmedia.fastfilm.databinding.DialogFirstRechargeBinding
import com.nhsd.basic.BaseVMDialogFragment


class FirstRechargeDialog : BaseVMDialogFragment<DialogFirstRechargeBinding, FirstRechargeViewModel>() {


    companion object {

        const val SCENE_HOME = 0
        const val SCENE_OTHER = 1

        fun show(firstRechargeDialogData: FirstRechargeDialogData?): FirstRechargeDialog? {
            val activity = ActivityUtils.getTopActivity() as? FragmentActivity ?: return null
            val dialog = FirstRechargeDialog().apply {
                arguments = bundleOf(Constants.Extra.DATA to firstRechargeDialogData)
            }
            dialog.show(activity.supportFragmentManager)
            return dialog
        }
    }
}