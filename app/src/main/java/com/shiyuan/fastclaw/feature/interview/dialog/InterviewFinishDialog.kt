package com.shiyuan.fastclaw.feature.interview.dialog

import android.content.Context
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogInterviewFinishBinding

/**
 * @desc：面试是否结束弹框
 * @author：CaoWei
 * @date：2026/6/23 15:49
 */
class InterviewFinishDialog(context: Context) : CenterPopupView(context) {

    private lateinit var mBinding: DialogInterviewFinishBinding
    var listener: (() -> Unit)? = null

    override fun getImplLayoutId(): Int = R.layout.dialog_interview_finish

    override fun onCreate() {
        mBinding = DialogInterviewFinishBinding.bind(popupImplView)
        mBinding.sbBack.setOnSingleClickListener {
            dismiss()
        }
        mBinding.tvFinish.setOnSingleClickListener {
            listener?.invoke()
            dismiss()
        }
    }

    fun showDialog(): InterviewFinishDialog {
        XPopup.Builder(activity)
            .isDestroyOnDismiss(false)
            .dismissOnBackPressed(false)
            .dismissOnTouchOutside(false)
            .asCustom(this)
            .show()
        return this
    }
}