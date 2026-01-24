package com.kgmedia.fastfilm.feature.login

import android.content.DialogInterface
import androidx.fragment.app.FragmentActivity
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.databinding.DialogSimpleProfileBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 选择性别和年龄
 */
class SimpleProfileDialog : BaseBottomPushVMDialogFragment<DialogSimpleProfileBinding, SimpleProfileViewModel>() {
    companion object {
        fun requireShow(onDismissListener: DialogInterface.OnDismissListener): Boolean {
            val manager = (topActivity as? FragmentActivity)?.supportFragmentManager ?: return false
            if (Global.needCompleteProfile) {
                Global.needCompleteProfile = false
                SimpleProfileDialog()
                    .apply {
                        isCancelable = false
                        setOnDismissListener(onDismissListener)
                    }
                    .show(manager)
                return true
            }
            return false
        }
    }

}