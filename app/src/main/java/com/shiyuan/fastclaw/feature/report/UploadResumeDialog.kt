package com.shiyuan.fastclaw.feature.report

import android.view.View
import androidx.fragment.app.FragmentManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogUploadResumeBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast

/**
 * 上传简历dialog
 * 新增：通过 tvTrue 选择本地文件（PDF/Word/图片）并回调
 */
class UploadResumeDialog : BaseBottomPushVMDialogFragment<DialogUploadResumeBinding, ReportViewModel>() {

    companion object {
        const val TAG = "UploadResumeDialog"
        var onFileSelected: ((SelectedFileInfo?) -> Unit)? = null

        fun show(manager: FragmentManager, fileSelectedListener: ((SelectedFileInfo?) -> Unit)? = null): UploadResumeDialog {
            val dialog = UploadResumeDialog()
            onFileSelected = fileSelectedListener
            dialog.show(manager)
            return dialog
        }
    }

    private lateinit var fileChooser: FileChooser

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        dataUpdate()
        initFileChooser()
    }

    private fun initView() {
        binding.ivClose.setOnSingleClickListener { dismiss() }
        binding.llWx.setOnSingleClickListener { performWxUpload() }
        binding.llFolder.setOnSingleClickListener {  fileChooser.openFileChooser()}
    }

    private fun initFileChooser() {
        fileChooser = FileChooser(
            fragment = this,
            maxSizeBytes = 10 * 1024 * 1024, // 10MB
            onResult = { fileInfo ->
                if (fileInfo != null) {
                    onFileSelected?.invoke(fileInfo)
                    "已选择: ${fileInfo.fileName}".showToast()
                    dismiss()
                } else {
                    // 这里只处理非大小超限的错误（例如无法读取文件）
                    "文件选择失败".showToast()
                }
            },
            onSizeExceeded = {
                "文件大小不能超过10MB".showToast()
            }
        )
    }

    private fun performWxUpload() {
        R.string.wx_upload_developing.getResString().showToast()
    }

    private fun dataUpdate() {}

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
    }
}