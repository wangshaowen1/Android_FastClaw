package com.shiyuan.fastclaw.feature.report
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogUploadResumeBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.databinding.DialogResumeListBinding
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog.Companion.onFileSelected
import com.umeng.analytics.pro.da


/**
 * 简历列表dialog
 */
class ResumeListDialog : BaseBottomPushVMDialogFragment<DialogResumeListBinding, ReportViewModel>(){

    companion object {
        const val TAG = "UploadResumeDialog"

        var onUpload: (() -> Unit)? = null
        var onTrue: (() -> Unit)? = null

        fun show(manager: FragmentManager, data: ResumeData, onUpload: (() -> Unit)? = null, onTrue: (() -> Unit)? = null): ResumeListDialog {
            this.onUpload = onUpload
            this.onTrue = onTrue
            val dialog = ResumeListDialog()
            val args = Bundle()
            args.putParcelable("data", data)
            dialog.arguments = args
            dialog.show(manager)
            return dialog
        }
    }


    override fun onViewModelCreated() {
        super.onViewModelCreated()
        val data = arguments?.getParcelable<ResumeData>("data")
        initView()
        dataUpdate(data)
    }



    private fun initView() {
        binding.ivClose.setOnSingleClickListener {
            dismiss()
        }

        binding.tvUpload.setOnSingleClickListener {
            onUpload?.let {
                dismiss()
                it.invoke()
            }
        }

        binding.tvTrue.setOnSingleClickListener {
            onTrue?.let {
                dismiss()
                it.invoke()
            }

        }
    }




    private fun dataUpdate(data:ResumeData?) {
        data?.let {
            binding.tvName.text = it.resumeFileName
            binding.tvTime.text = "上传于 "+ it.submitTime

        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
    }
}
