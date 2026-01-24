package com.kgmedia.fastfilm.feature.timbre

import android.os.Bundle
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.databinding.DialogCreateCloneTimbreBinding
import com.kgmedia.fastfilm.feature.payment.points.PointsRechargeActivity
import com.kgmedia.fastfilm.feature.payment.vip.MembershipBenefitsActivity
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 新建克隆音色弹窗
 */
class CreateCloneTimbreDialog : BaseBottomPushVMDialogFragment<DialogCreateCloneTimbreBinding, CreateCloneTimbreViewModel>() {

    companion object {
        const val AUDIO_PATH = "AUDIO_PATH"
        const val AUDIO_DURING = "AUDIO_DURING"
        fun newInstance(audioPath: String?, during: Long): CreateCloneTimbreDialog {
            val args = Bundle()
            args.putString(AUDIO_PATH, audioPath)
            args.putLong(AUDIO_DURING, during)
            val fragment = CreateCloneTimbreDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onResume() {
        super.onResume()
        mRealVM.getCost()
    }

    var onConfirmClick: ((Boolean) -> Unit)? = null

    override fun onPrepareView() {
        super.onPrepareView()
        binding.btnConfirm.setOnClickListener {
            if (!User.get().isVip) {
                MembershipBenefitsActivity.start(activity, MembershipBenefitsActivity.Scene.CloneVoice)
                return@setOnClickListener
            }
            if (mRealVM.costData.get() == null) {
                mRealVM.getCost()
                return@setOnClickListener
            }
            if (mRealVM.costData.get()?.isBalanceEnough() == false) {
                PointsRechargeActivity.start(activity)
                return@setOnClickListener
            }
            onConfirmClick?.invoke(true)
            dismissAllowingStateLoss()
        }
    }
}