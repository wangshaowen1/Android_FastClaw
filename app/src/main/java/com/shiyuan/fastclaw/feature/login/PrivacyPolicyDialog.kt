package com.shiyuan.fastclaw.feature.login

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.databinding.DialogPayPolicyBinding
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener


/**
 * 登录协议弹窗
 * @author xiaoowuu
 */
class PrivacyPolicyDialog : BaseDialogFragment<DialogPayPolicyBinding>() {

    companion object {
        private const val ARG_AGREE_TEXT = "agree_text"

        fun show(manager: FragmentManager, agreeText: String? = null): PrivacyPolicyDialog {
            val dialog = PrivacyPolicyDialog()
            agreeText?.let {
                dialog.arguments = Bundle().apply {
                    putString(ARG_AGREE_TEXT, it)
                }
            }
            dialog.show(manager)
            return dialog
        }
    }


    //点击同意或不同意回调
    var callback: ((agree: Boolean) -> Unit)? = null


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


    @SuppressLint("SetTextI18n")
    override fun onPrepareView() {
        super.onPrepareView()
        binding.btnAgree.typeface = Typeface.DEFAULT_BOLD
        binding.btnReject.typeface = Typeface.DEFAULT_BOLD

        val contentStr = R.string.policy_content.getResString()
        val text = SpannableStringBuilder(contentStr)
        text.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                SimpleBrowserActivity.launch(
                    this@PrivacyPolicyDialog.activity,
                    Config.get().serviceUrl,
                    R.string.login_user_agreement.getResString()
                )
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.setColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.login_agreement_link
                    )
                )
                ds.isUnderlineText = false
            }
        }, 25, 34, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                SimpleBrowserActivity.launch(
                    this@PrivacyPolicyDialog.activity,
                    Config.get().privacyUrl,
                    R.string.login_privacy_policy.getResString()
                )
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.setColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.login_agreement_link
                    )
                )
                ds.isUnderlineText = false
            }
        }, 34, 40, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
        binding.tvContent.text = text

        binding.btnReject.text = R.string.app_not_agree_and_quit.getResString()
        binding.btnAgree.text = arguments?.getString(ARG_AGREE_TEXT)
            ?: R.string.app_agree_and_sign_in.getResString()

        binding.btnAgree.setOnSingleClickListener {
            dismiss()
            callback?.invoke(true)
        }
        binding.btnReject.setOnSingleClickListener {
            dismiss()
            callback?.invoke(false)
        }
    }
}