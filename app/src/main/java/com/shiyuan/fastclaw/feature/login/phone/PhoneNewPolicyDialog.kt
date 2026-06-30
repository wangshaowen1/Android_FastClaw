package com.shiyuan.fastclaw.feature.login.phone

import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.databinding.DialogPhoneNewPolicyBinding
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity

class PhoneNewPolicyDialog : BaseDialogFragment<DialogPhoneNewPolicyBinding>() {

    companion object {
        /**
         * 展示新版登录协议确认弹窗。
         */
        fun show(manager: FragmentManager): PhoneNewPolicyDialog {
            return PhoneNewPolicyDialog().apply {
                show(manager)
            }
        }
    }

    var callback: ((agree: Boolean) -> Unit)? = null

    /**
     * 设置弹窗宽度和居中展示位置。
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
     * 初始化协议富文本和同意/不同意按钮事件。
     */
    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
        binding.tvContent.highlightColor = android.graphics.Color.TRANSPARENT
        binding.tvContent.text = buildContent()
        binding.btnAgree.setOnSingleClickListener {
            dismissAllowingStateLoss()
            callback?.invoke(true)
        }
        binding.btnReject.setOnClickListener {
            dismissAllowingStateLoss()
            callback?.invoke(false)
        }
    }

    /**
     * 生成协议确认正文，并给协议名称增加点击跳转。
     */
    private fun buildContent(): SpannableString {
        val service = getString(R.string.login_new_policy_user_service)
        val privacy = getString(R.string.login_new_policy_privacy)
        val terms = getString(R.string.login_new_policy_terms)
        val text = SpannableString(getString(R.string.login_new_policy_content, service, privacy, terms))
        addLink(text, service, Config.get().serviceUrl, R.string.login_new_policy_user_service)
        addLink(text, privacy, Config.get().privacyUrl, R.string.login_new_policy_privacy)
        addLink(text, terms, Config.get().serviceUrl, R.string.login_new_policy_terms)
        return text
    }

    /**
     * 给协议正文中的指定协议名称增加点击事件。
     */
    private fun addLink(text: SpannableString, target: String, url: String?, titleRes: Int) {
        val start = text.indexOf(target)
        if (start < 0) return
        text.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    SimpleBrowserActivity.launch(this@PhoneNewPolicyDialog.activity, url, getString(titleRes))
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(requireContext(), R.color.color_336fff)
                    ds.isUnderlineText = false
                }
            },
            start,
            start + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
