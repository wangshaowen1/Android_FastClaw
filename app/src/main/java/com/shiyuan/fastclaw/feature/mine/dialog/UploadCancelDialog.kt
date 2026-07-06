package com.shiyuan.fastclaw.feature.mine.dialog

import androidx.fragment.app.FragmentManager
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.base.BaseBottomDialogFragment
import com.shiyuan.fastclaw.base.BaseBottomDialogVmFragment
import com.shiyuan.fastclaw.databinding.DialogUploadCancelBinding
import com.shiyuan.fastclaw.feature.report.ReportViewModel

/**
 * 上传取消dialog
 *
 */
class UploadCancelDialog : BaseBottomDialogVmFragment<DialogUploadCancelBinding, ReportViewModel>() {

    companion object {
        const val TAG = "UploadCancelDialog"
        var uploadListener: (() -> Unit)? = null

        fun show(manager: FragmentManager, uploadListener: (() -> Unit)? = null): UploadCancelDialog {
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
