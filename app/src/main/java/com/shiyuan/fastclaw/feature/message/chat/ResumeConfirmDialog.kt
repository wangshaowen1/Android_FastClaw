package com.shiyuan.fastclaw.feature.message.chat

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.BaseBottomDialogFragment
import com.shiyuan.fastclaw.core.network.apis.response.ResumeListData
import com.shiyuan.fastclaw.databinding.DialogMessageResumeConfirmBinding

/**
 * 发送附件简历确认弹框。
 */
class ResumeConfirmDialog : BaseBottomDialogFragment<DialogMessageResumeConfirmBinding>() {

    // 点击发送后的页面回调。
    private var onSendClick: ((ResumeListData) -> Unit)? = null

    companion object {
        // 简历数据参数。
        private const val EXTRA_RESUME = "extra_resume"

        /**
         * 展示发送附件简历确认弹框。
         */
        fun show(manager: FragmentManager, resume: ResumeListData, onSendClick: (ResumeListData) -> Unit): ResumeConfirmDialog {
            val dialog = ResumeConfirmDialog()
            dialog.arguments = Bundle().apply {
                putParcelable(EXTRA_RESUME, resume)
            }
            dialog.onSendClick = onSendClick
            dialog.show(manager)
            return dialog
        }
    }

    /**
     * 初始化简历展示内容和按钮事件。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val resume = arguments?.getParcelable<ResumeListData>(EXTRA_RESUME) ?: return
        binding.tvFileName.text = resume.resumeName ?: resume.fileName.orEmpty()
        binding.tvUploadTime.text = buildUploadTime(resume)
        binding.tvUploadTime.visibility = if (binding.tvUploadTime.text.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.ivFile.setImageResource(resolveResumeIcon(resume))
        binding.ivClose.setOnSingleClickListener { dismiss() }
        binding.tvCancel.setOnSingleClickListener { dismiss() }
        binding.tvSend.setOnSingleClickListener {
            dismiss()
            onSendClick?.invoke(resume)
        }
    }

    /**
     * 组装简历上传时间文案。
     */
    private fun buildUploadTime(resume: ResumeListData): String {
        val uploadTime = resume.uploadTime?.takeIf { it.isNotBlank() }
            ?: resume.updateTime?.takeIf { it.isNotBlank() }
            ?: return ""
        return getString(R.string.resume_confirm_upload_time, uploadTime)
    }

    /**
     * 根据简历格式选择文件图标。
     */
    private fun resolveResumeIcon(resume: ResumeListData): Int {
        val fileInfo = "${resume.fileType.orEmpty()} ${resume.resumeName.orEmpty()} ${resume.fileName.orEmpty()}"
        return if (fileInfo.contains("pdf", ignoreCase = true)) {
            R.drawable.ic_resume
        } else {
            R.drawable.ic_file_word
        }
    }
}
