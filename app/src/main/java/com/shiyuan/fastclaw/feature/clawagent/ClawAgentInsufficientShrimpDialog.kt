package com.shiyuan.fastclaw.feature.clawagent

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import androidx.fragment.app.FragmentManager
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogClawAgentInsufficientShrimpBinding

/** 虾条不足底部浮层 */
class ClawAgentInsufficientShrimpDialog :
    BaseBottomPushDialogFragment<DialogClawAgentInsufficientShrimpBinding>() {

    companion object {
        private const val KEY_REMAINING_COUNT = "remainingCount"
        private const val DEFAULT_REMAINING_COUNT = 2

        fun show(
            manager: FragmentManager,
            remainingCount: Int = DEFAULT_REMAINING_COUNT,
        ): ClawAgentInsufficientShrimpDialog {
            return ClawAgentInsufficientShrimpDialog().apply {
                arguments = Bundle().apply {
                    putInt(KEY_REMAINING_COUNT, remainingCount)
                }
                show(manager)
            }
        }
    }

    var onInviteClick: (() -> Unit)? = null
    var onRechargeClick: (() -> Unit)? = null

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvTitle.text = buildTitleText(arguments?.getInt(KEY_REMAINING_COUNT) ?: DEFAULT_REMAINING_COUNT)
        binding.ivClose.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvInvite.setOnSingleClickListener {
            dismissAllowingStateLoss()
            onInviteClick?.invoke()
        }
        binding.tvRecharge.setOnSingleClickListener {
            dismissAllowingStateLoss()
            onRechargeClick?.invoke()
        }
    }

    private fun buildTitleText(remainingCount: Int): SpannableString {
        val suffix = getString(R.string.str_agent_insufficient_shrimp_remaining, remainingCount)
        val title = getString(R.string.str_agent_insufficient_shrimp_title, suffix)
        return SpannableString(title).apply {
            val start = title.indexOf(suffix)
            if (start >= 0) {
                setSpan(
                    AbsoluteSizeSpan(11, true),
                    start,
                    start + suffix.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
}
