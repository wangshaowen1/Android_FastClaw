package com.shiyuan.fastclaw.feature.splash

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import com.blankj.utilcode.util.ScreenUtils
import com.drake.spannable.addSpan
import com.drake.spannable.span.HighlightSpan
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.databinding.singleClick
import com.shiyuan.fastclaw.databinding.DialogPolicyBinding
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.getThemeColor
import kotlin.math.min


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

    private var highlightColor = R.color.theme_color_2_dark.getResColor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        activity?.getThemeColor(R.attr.themeColor2)?.let { highlightColor = it }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                val screenWidth = min(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight())
                attr.width = (293 / 375f * screenWidth).toInt()
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

    /**
     * 《隐私政策》
     */
    private fun CharSequence.addPrivacyPolicy() = addSpan(
        R.string.tvPrivateService.getResString(),
        HighlightSpan(highlightColor) {
            activity?.let {
                SimpleBrowserActivity.launch(
                    it,
                    Config.get().privacyUrl,
                    R.string.tvSYPrivateService.getResString()
                )
            }
        })

    /**
     * 《用户协议》
     */
    private fun CharSequence.addUserPolicy() = addSpan(
        R.string.tvUserService.getResString(),
        HighlightSpan(highlightColor) {
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
            "为了更好地保障您的合法权益，请您阅读并同意以下协议"
                .addUserPolicy()
                .addSpan("和")
                .addPrivacyPolicy()
                .addSpan("未注册手机号验证登录后自动创建账号。")
        singleClick(binding.tvAgree) { (activity as? PolicyCallback)?.onAgreePolicy(this) }
        singleClick(binding.tvReject) { (activity as? PolicyCallback)?.onRejectPolicy(this) }
    }

    interface PolicyCallback {
        fun onAgreePolicy(dialog: PolicyDialog)
        fun onRejectPolicy(dialog: PolicyDialog)
    }
}

