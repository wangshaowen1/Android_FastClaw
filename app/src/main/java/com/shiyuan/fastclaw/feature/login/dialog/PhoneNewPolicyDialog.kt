package com.shiyuan.fastclaw.feature.login.dialog

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
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
        private const val ARG_OPERATOR_NAME = "operatorName"
        private const val ARG_OPERATOR_URL = "operatorUrl"

        /**
         * 展示新登录协议确认弹框。
         */
        fun show(
            manager: FragmentManager,
            operatorName: String? = null,
            operatorUrl: String? = null
        ): PhoneNewPolicyDialog {
            return PhoneNewPolicyDialog().apply {
                arguments = android.os.Bundle().apply {
                    putString(ARG_OPERATOR_NAME, operatorName)
                    putString(ARG_OPERATOR_URL, operatorUrl)
                }
                show(manager)
            }
        }

        /**
         * 在闪验授权 Activity 上展示协议确认弹框，避免 Fragment 弹框被授权页遮挡。
         */
        fun show(
            activity: Activity?,
            operatorName: String?,
            operatorUrl: String?,
            callback: (agree: Boolean) -> Unit
        ): Dialog? {
            if (activity == null || activity.isFinishing || activity.isDestroyed) return null
            val binding = DialogPhoneNewPolicyBinding.inflate(LayoutInflater.from(activity))
            val dialog = Dialog(activity, R.style.LoadingDialog).apply {
                setContentView(binding.root)
                setCanceledOnTouchOutside(true)
            }
            initContent(
                binding = binding,
                activity = activity,
                operatorName = operatorName,
                operatorUrl = operatorUrl,
                onAgree = {
                    dialog.dismiss()
                    callback(true)
                },
                onReject = {
                    dialog.dismiss()
                    callback(false)
                }
            )
            dialog.show()
            dialog.window?.let { dialogWindow ->
                dialogWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                dialogWindow.attributes?.let { attr ->
                    attr.width = (ScreenUtils.getScreenWidth() * 296f / 375f).toInt()
                    attr.gravity = Gravity.CENTER
                    attr.dimAmount = 0.5f
                    dialogWindow.attributes = attr
                }
            }
            return dialog
        }

        private fun initContent(
            binding: DialogPhoneNewPolicyBinding,
            activity: Activity,
            operatorName: String?,
            operatorUrl: String?,
            onAgree: () -> Unit,
            onReject: () -> Unit
        ) {
            binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
            binding.tvContent.highlightColor = Color.TRANSPARENT
            binding.tvContent.text = buildFlashContent(activity, operatorName, operatorUrl)
            binding.btnAgree.setOnSingleClickListener { onAgree() }
            binding.btnReject.setOnClickListener { onReject() }
        }

        private fun buildFlashContent(
            activity: Activity,
            operatorName: String?,
            operatorUrl: String?
        ): SpannableString {
            val service = activity.getString(R.string.flash_new_service_agreement)
            val privacy = activity.getString(R.string.flash_new_privacy_policy)
            val operator = operatorName ?: activity.getString(R.string.flash_new_account_terms)
            return SpannableString(
                activity.getString(R.string.login_new_policy_flash_content, service, privacy, operator)
            ).apply {
                addLink(activity, this, service, Config.get().serviceUrl, R.string.flash_new_service_agreement)
                addLink(activity, this, privacy, Config.get().privacyUrl, R.string.flash_new_privacy_policy)
                addLink(activity, this, operator, operatorUrl ?: Config.get().serviceUrl, 0)
            }
        }

        private fun addLink(
            activity: Activity,
            text: SpannableString,
            target: String,
            url: String?,
            titleRes: Int
        ) {
            val start = text.indexOf(target)
            if (start < 0) return
            text.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        SimpleBrowserActivity.launch(
                            activity,
                            url,
                            if (titleRes == 0) target else activity.getString(titleRes)
                        )
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(activity, R.color.color_2f6bff)
                        ds.isUnderlineText = false
                    }
                },
                start,
                start + target.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    var callback: ((agree: Boolean) -> Unit)? = null

    /**
     * 设置弹框宽度和居中展示位置。
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
        binding.tvContent.highlightColor = Color.TRANSPARENT
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
        val operatorName = arguments?.getString(ARG_OPERATOR_NAME)
        val operatorUrl = arguments?.getString(ARG_OPERATOR_URL)
        if (!operatorName.isNullOrBlank()) {
            val service = getString(R.string.flash_new_service_agreement)
            val privacy = getString(R.string.flash_new_privacy_policy)
            return SpannableString(
                getString(R.string.login_new_policy_flash_content, service, privacy, operatorName)
            ).apply {
                addLink(this, service, Config.get().serviceUrl, R.string.flash_new_service_agreement)
                addLink(this, privacy, Config.get().privacyUrl, R.string.flash_new_privacy_policy)
                addLink(this, operatorName, operatorUrl, 0)
            }
        }
        val service = getString(R.string.login_new_policy_user_service)
        val privacy = getString(R.string.login_new_policy_privacy)
        val terms = getString(R.string.login_new_policy_terms)
        val text =
            SpannableString(getString(R.string.login_new_policy_content, service, privacy, terms))
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
                    SimpleBrowserActivity.launch(
                        activity,
                        url,
                        if (titleRes == 0) target else getString(titleRes)
                    )
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(requireContext(), R.color.color_2f6bff)
                    ds.isUnderlineText = false
                }
            },
            start,
            start + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
