package com.shiyuan.fastclaw.feature.message.dialog

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogMessageJobSwitchConfirmBinding

/**
 * 切换沟通职位二次确认弹框。
 */
class JobSwitchConfirmDialog : BaseDialogFragment<DialogMessageJobSwitchConfirmBinding>() {

    // 点击切换职位后的页面回调。
    private var onSwitchClick: (() -> Unit)? = null

    companion object {
        // 新职位名称参数。
        private const val EXTRA_JOB_NAME = "extra_job_name"

        /**
         * 展示切换沟通职位二次确认弹框。
         */
        fun show(manager: FragmentManager, jobName: String?, onSwitchClick: () -> Unit): JobSwitchConfirmDialog {
            val dialog = JobSwitchConfirmDialog()
            dialog.arguments = Bundle().apply {
                putString(EXTRA_JOB_NAME, jobName)
            }
            dialog.onSwitchClick = onSwitchClick
            dialog.show(manager)
            return dialog
        }
    }

    /**
     * 按设计稿设置居中弹框宽度。
     */
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = (ScreenUtils.getScreenWidth() * 296f / 375f).toInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    /**
     * 初始化弹框文案和按钮点击事件。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val jobName = arguments?.getString(EXTRA_JOB_NAME).orEmpty()
        binding.tvContent.text = buildContent(jobName)
        binding.tvCancel.setOnSingleClickListener { dismiss() }
        binding.tvSwitch.setOnSingleClickListener {
            onSwitchClick?.invoke()
            dismiss()
        }
    }

    /**
     * 组装正文，并高亮新职位名称。
     */
    private fun buildContent(jobName: String): SpannableString {
        val content = getString(R.string.message_chat_job_switch_content, jobName)
        val spannable = SpannableString(content)
        val start = content.indexOf(jobName)
        if (start >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.color_2f6bff)),
                start,
                start + jobName.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }
}
