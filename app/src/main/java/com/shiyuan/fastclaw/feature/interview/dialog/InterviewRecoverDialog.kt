package com.shiyuan.fastclaw.feature.interview.dialog

import android.content.Context
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogInterviewRecoverBinding

/**
 * @desc：面试恢复弹框
 * @author：CaoWei
 * @date：2026/6/24 16:10
 */
class InterviewRecoverDialog(context: Context) : CenterPopupView(context) {

    private lateinit var mBinding: DialogInterviewRecoverBinding
    var onContinueClickListener: (() -> Unit)? = null
    var onFinishClickListener: (() -> Unit)? = null

    override fun getImplLayoutId(): Int = R.layout.dialog_interview_recover

    override fun onCreate() {
        mBinding = DialogInterviewRecoverBinding.bind(popupImplView)
        mBinding.sbContinue.setOnSingleClickListener {
            onContinueClickListener?.invoke()
            dismiss()
        }
        mBinding.tvFinish.setOnSingleClickListener {
            onFinishClickListener?.invoke()
            dismiss()
        }
    }

    fun showDialog(): InterviewRecoverDialog {
        XPopup.Builder(activity)
            .isDestroyOnDismiss(false)
            .dismissOnBackPressed(false)
            .dismissOnTouchOutside(false)
            .asCustom(this)
            .show()
        return this
    }
}
