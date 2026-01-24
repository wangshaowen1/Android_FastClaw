package com.kgmedia.fastfilm.feature.payment.vip

import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.databinding.DialogMembershipInstructionsBinding
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:会员说明弹窗
 * @Data: 2025/11/4/周二  9:50
 * @Version: 1.0
 * *****************************************
 */
class MembershipInstructionsDialog :
    BaseBottomPushDialogFragment<DialogMembershipInstructionsBinding>() {

    companion object {
        fun show(manager: FragmentManager): MembershipInstructionsDialog {
            val dialog = MembershipInstructionsDialog()
            dialog.show(manager)
            return dialog
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.ivClose.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
    }
}