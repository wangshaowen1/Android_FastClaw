package com.shiyuan.fastclaw.feature.report
import android.view.View
import androidx.fragment.app.FragmentManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogUploadResumeBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.databinding.DialogResumeListBinding


/**
 * 简历列表dialog
 */
class ResumeListDialog : BaseBottomPushVMDialogFragment<DialogResumeListBinding, ReportViewModel>(){

    companion object {
        const val TAG = "UploadResumeDialog"

        fun show(manager: FragmentManager): ResumeListDialog {
            val dialog = ResumeListDialog()
            dialog.show(manager)
            return dialog
        }
    }


    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        dataUpdate()
    }



    private fun initView() {
        binding.ivClose.setOnSingleClickListener {
            dismiss()
        }

        binding.tvUpload.setOnSingleClickListener {

        }

        binding.tvTrue.setOnSingleClickListener {

        }
    }




    private fun dataUpdate() {}

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
    }
}
