package com.kgmedia.fastfilm.feature.splash

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import com.drake.spannable.addSpan
import com.drake.spannable.span.HighlightSpan
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.databinding.singleClick
import com.kgmedia.fastfilm.databinding.DialogPolicyBinding
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString


/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:隐私政策和用户协议
 * @Data: 2025/9/9/周二  15:33
 * @Version: 1.0
 * *****************************************
 */
class PolicyDialog : BaseDialogFragment<DialogPolicyBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = 285.dp
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.Dialog_Common).apply {
            setCanceledOnTouchOutside(false)
        }
    }

    private fun CharSequence.addPrivacyPolicy() = addSpan(
        R.string.tvPrivateService.getResString(),
        HighlightSpan(R.color.color_ff4b00.getResColor()) {
            activity?.let {
                SimpleBrowserActivity.launch(
                    it,
                    Config.get().privacyUrl,
                    R.string.tvSYPrivateService.getResString()
                )
            }
        })

    private fun CharSequence.addUserPolicy() = addSpan(
        R.string.tvUserService.getResString(),
        HighlightSpan(R.color.color_ff4b00.getResColor()) {
            activity?.let {
                SimpleBrowserActivity.launch(
                    it,
                    Config.get().serviceUrl,
                    R.string.tvSYUserService.getResString()
                )
            }
        })

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
        binding.tvContent.text =
            "点击“同意并继续”代表您已阅读并理解《隐私政策》和《用户协议》,我们深知保护用户隐私信息的重要性，请认真仔细阅读。\n查看完整版"
                .addPrivacyPolicy()
                .addSpan("和")
                .addUserPolicy()
        singleClick(binding.tvAgree) {
            (activity as? PolicyCallback)?.onAgreePolicy(this)
        }
        singleClick(binding.tvReject) { (activity as? PolicyCallback)?.onRejectPolicy(this) }
    }

    interface PolicyCallback {
        fun onAgreePolicy(dialog: PolicyDialog)
        fun onRejectPolicy(dialog: PolicyDialog)
    }
}