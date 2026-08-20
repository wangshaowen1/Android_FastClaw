package com.shiyuan.fastclaw.feature.opportunity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.ext.customerServiceTransfer
import com.shiyuan.fastclaw.databinding.ActivityOpportunityDetailBinding
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityDetailActionState
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityViewModel
import io.noties.markwon.Markwon

/**
 * 机会详情。
 */
class OpportunityDetailActivity :
    BaseViewModelActivity<ActivityOpportunityDetailBinding, OpportunityViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            activity.startActivity(Intent(activity, OpportunityDetailActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.opportunityDetailActionState.value = OpportunityDetailActionState.UNSUITABLE
        mRealVM.opportunityDetailActionState.observe(this) { state ->
            renderActionState(state)
        }
        setupReportTip()
    }

    private fun setupReportTip() {
        val content = getString(R.string.opportunity_detail_security_tip_content)
        val reportText = getString(R.string.opportunity_detail_report_now)
        val reportStart = content.indexOf(reportText)
        if (reportStart < 0) return

        binding.tvTip.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTip.highlightColor = Color.TRANSPARENT
        binding.tvTip.text = SpannableString(content).apply {
            setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        customerServiceTransfer()
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.color = ContextCompat.getColor(
                            this@OpportunityDetailActivity,
                            R.color.color_2f6bff
                        )
                        ds.isUnderlineText = true
                    }
                },
                reportStart,
                reportStart + reportText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun renderActionState(state: OpportunityDetailActionState) {
        val isClosed = state == OpportunityDetailActionState.CLOSED
        binding.tvUnsuitable.setText(
            when (state) {
                OpportunityDetailActionState.UNSUITABLE -> R.string.opportunity_detail_temporarily_unsuitable
                OpportunityDetailActionState.CLOSED -> R.string.opportunity_detail_position_closed
                else -> R.string.opportunity_detail_unsuitable
            }
        )
        binding.tvUnsuitable.setTextColor(
            ContextCompat.getColor(
                this,
                if (isClosed) R.color.color_55_3c3c43 else R.color.color_2f6bff
            )
        )
        binding.tvUnsuitable.setBackgroundResource(
            if (isClosed) {
                R.drawable.bg_opportunity_detail_closed_button
            } else {
                R.drawable.bg_opportunity_detail_secondary_button
            }
        )
        binding.tvCommunicateNow.setText(
            if (state == OpportunityDetailActionState.COMMUNICATING) {
                R.string.opportunity_detail_continue_communicating
            } else {
                R.string.opportunity_detail_communicate_now
            }
        )
        binding.tvCommunicateNow.visibility =
            if (state == OpportunityDetailActionState.NEW_OPPORTUNITY ||
                state == OpportunityDetailActionState.COMMUNICATING
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }
}

@BindingAdapter("opportunityMarkdown")
fun setOpportunityMarkdown(view: TextView, markdown: String?) {
    Markwon.create(view.context).setMarkdown(view, markdown.orEmpty())
}
