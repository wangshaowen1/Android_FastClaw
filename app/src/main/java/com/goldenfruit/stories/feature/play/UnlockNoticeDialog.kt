package com.kgmedia.fastfilm.feature.play

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.databinding.DialogUnlockNoticeBinding
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseBottomPushDialogFragment

/**
 * 付费须知
 */
class UnlockNoticeDialog : BaseBottomPushDialogFragment<DialogUnlockNoticeBinding>() {

    companion object {
        fun show(
            manager: FragmentManager,
        ): UnlockNoticeDialog {
            val dialog = UnlockNoticeDialog()
            dialog.show(manager)
            return dialog
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        setupTip()
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }
    }

    fun setupTip() {
        val originalText = Config.get().appConfig.paymentNotice ?: return
        val spannable = SpannableString(originalText)
        val linkColor = "#ff6b2e".toColorInt()

        spannable.addClickableLink("《充值协议》", linkColor) {
            SimpleBrowserActivity.launch(activity, Config.get().appConfig.clientPaymentProtocolUrl)
        }

        spannable.addClickableLink("《付费会员协议》", linkColor) {
            SimpleBrowserActivity.launch(activity, Config.get().appConfig.userVipProtocolUrl)
        }

        binding.tvNotice.apply {
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
            text = spannable
        }
    }

    /**
     * SpannableString 的扩展函数：查找指定文本并添加点击事件和颜色
     */
    private fun SpannableString.addClickableLink(
        targetText: String,
        color: Int,
        onClickAction: () -> Unit
    ) {
        val startIndex = this.toString().indexOf(targetText)
        if (startIndex != -1) {
            val endIndex = startIndex + targetText.length

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onClickAction()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = color
                    ds.isUnderlineText = false // 去掉下划线
                }
            }
            this.setSpan(clickableSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}