package com.shiyuan.fastclaw.feature.mine

import android.view.View
import androidx.fragment.app.FragmentManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogUploadResumeBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.databinding.DialogUploadCancelBinding
import com.shiyuan.fastclaw.feature.report.FileChooser
import com.shiyuan.fastclaw.feature.report.ReportViewModel
import com.shiyuan.fastclaw.feature.report.SelectedFileInfo

/**
 * 上传取消dialog
 *
 */
class UploadCancelDialog : BaseBottomPushVMDialogFragment<DialogUploadCancelBinding, ReportViewModel>() {

    companion object {
        const val TAG = "UploadCancelDialog"
        var uploadListener: (() -> Unit)? = null

        fun show(manager: FragmentManager,uploadListener: (() -> Unit)? = null): UploadCancelDialog {
            val dialog = UploadCancelDialog()
            this.uploadListener = uploadListener
            dialog.show(manager)
            return dialog
        }
    }


    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
    }

    private fun initView() {
        binding.tvCancel.setOnSingleClickListener { dismiss() }
        binding.tvUpload.setOnSingleClickListener {
            uploadListener?.invoke()
            dismiss()
        }
    }


}