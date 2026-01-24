package com.kgmedia.fastfilm.feature.payment.points

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.DialogFirstTimeUnlockBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 解锁首次作品弹窗
 */
class FirstTimeUnlockDialog : BaseBottomPushVMDialogFragment<DialogFirstTimeUnlockBinding, FirstTimeUnlockViewModel>() {
    companion object {
        fun show(fragmentManager: FragmentManager?, dramasId: String?, cost: Int): FirstTimeUnlockDialog? {
            val fragmentManager = fragmentManager ?: (topActivity as? FragmentActivity)?.supportFragmentManager ?: return null
            return FirstTimeUnlockDialog().apply {
                arguments = Bundle().apply {
                    putString(Constants.Extra.DRAMAS_ID, dramasId)
                    putInt(Constants.Extra.COST, cost)
                }
                show(fragmentManager)
            }
        }
    }
}