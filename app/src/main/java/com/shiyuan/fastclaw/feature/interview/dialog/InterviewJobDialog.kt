package com.shiyuan.fastclaw.feature.interview.dialog

import android.content.Context
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BottomPopupView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogInterviewJobBinding

/**
 * @desc：目标岗位截图
 * @author：CaoWei
 * @date：2026/6/23 17:26
 */
class InterviewJobDialog(context: Context) : BottomPopupView(context) {

    private lateinit var mBinding: DialogInterviewJobBinding
    var listener: (() -> Unit)? = null

    override fun getImplLayoutId(): Int = R.layout.dialog_interview_job

    override fun onCreate() {
        mBinding = DialogInterviewJobBinding.bind(popupImplView)
        mBinding.ivClose.setOnSingleClickListener {
            dismiss()
        }
    }

    fun showDialog(): InterviewJobDialog {
        XPopup.Builder(context)
            .isViewMode(true)
            .asCustom(this)
            .show()
        return this
    }
}