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
import androidx.core.content.ContextCompat
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.BR
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.ext.customerServiceTransfer
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityDetailData
import com.shiyuan.fastclaw.databinding.ActivityOpportunityDetailBinding
import com.shiyuan.fastclaw.feature.message.chat.MessageChatActivity
import com.shiyuan.fastclaw.feature.message.data.MessageChatParams
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityDetailActionState
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityDetailViewModel
import io.noties.markwon.Markwon

/**
 * 机会详情页。
 *
 * 展示 [OpportunityDetailData]，并根据服务端状态和 actions 渲染底部操作。
 */
class OpportunityDetailActivity :
    BaseViewModelActivity<ActivityOpportunityDetailBinding, OpportunityDetailViewModel>() {

    private val markwon by lazy { Markwon.create(this) }

    companion object {
        private const val EXTRA_JOB_ID = "extra_job_id"
        private const val EXTRA_UNREAD = "extra_unread"
        private const val ACTION_MARK_COMMUNICATING = "MARK_COMMUNICATING"

        /**
         * 打开指定岗位的机会详情。
         *
         * @param jobId 岗位 ID，空值时不跳转。
         * @param unread 当前机会是否未读；详情加载成功后通知列表刷新已读状态。
         */
        fun start(activity: Activity?, jobId: String, unread: Boolean = false) {
            if (activity == null || jobId.isBlank()) return
            activity.startActivity(
                Intent(activity, OpportunityDetailActivity::class.java)
                    .putExtra(EXTRA_JOB_ID, jobId)
                    .putExtra(EXTRA_UNREAD, unread)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbar.setVariable(BR.toolbar, mRealVM.mToolbarVMOpportunityDetail)
        val jobId = intent.getStringExtra(EXTRA_JOB_ID).orEmpty()
        if (jobId.isBlank()) {
            finish()
            return
        }
        setupReportTip()
        // 返回 OpportunityDetailData 后统一渲染详情，并处理一次性的已读通知。
        mRealVM.opportunityDetail.observe(this) { detail ->
            renderDetail(detail)
            notifyOpportunityReadIfNeeded()
        }
        mRealVM.queryOpportunityDetail(jobId)
    }

    /** 首次成功展示未读机会后，通知机会列表重新查询已读状态。 */
    private fun notifyOpportunityReadIfNeeded() {
        if (!intent.getBooleanExtra(EXTRA_UNREAD, false)) return
        intent.putExtra(EXTRA_UNREAD, false)
        FlowEventBus.postSticky(Events.OpportunityListRefreshEvent(1))
    }

    /** 将机会详情响应渲染到页面，并继续处理公司入口和底部操作。 */
    private fun renderDetail(detail: OpportunityDetailData) {
        binding.tvOpportunityTitle.text = detail.jobTitle
        binding.tvOpportunityCompany.text = detail.companyName
        binding.vMatchPercent.progress = detail.matchPercent
        binding.tvOpportunityLocation.text = detail.workCity
        binding.tvOpportunityExperience.text = detail.experienceText
        binding.tvOpportunityEducation.text = detail.degreeText
        binding.tvOpportunitySalary.text = detail.salaryText
        binding.tvOpportunityIntroduction.visibility = if (detail.recommendationReason.isBlank()) View.GONE else View.VISIBLE
        markwon.setMarkdown(
            binding.tvOpportunityIntroduction,
            detail.recommendationReason
        )
        markwon.setMarkdown(binding.tvJobDescription, detail.jobDescription)
        binding.tvLocationCompany.text = detail.companyName
        binding.tvCompanyProfile.text = listOf(
            detail.companyIndustry,
            detail.companyScale,
            detail.financingStage
        ).filter(String::isNotBlank).joinToString(" · ")
        binding.tvWorkAddress.text = detail.workAddress
        binding.clXq.setOnSingleClickListener {
            CompanyDetailActivity.start(this, detail.enterpriseId)
        }
        renderBottomActions(detail)
    }

    /** 为安全提示中的“立即举报”添加企业微信客服点击能力。 */
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

    /** 使用响应中的状态和 actions 渲染底部按钮文案、可用性及点击事件。 */
    private fun renderBottomActions(detail: OpportunityDetailData) {
        val state = resolveActionState(detail)
        val firstAction = detail.actions.getOrNull(0)
        val secondAction = detail.actions.getOrNull(1)
        val showSecondAction = state == OpportunityDetailActionState.NEW_OPPORTUNITY || state == OpportunityDetailActionState.COMMUNICATING

        binding.layoutBottomAction.visibility = if (state == null) View.GONE else View.VISIBLE
        binding.tvUnsuitable.apply {
            text = firstAction?.buttonText.orEmpty()
            isEnabled = state != OpportunityDetailActionState.CLOSED && firstAction?.actionCode != null
            setOnSingleClickListener {
                firstAction?.actionCode?.let { actionCode ->
                    mRealVM.updateOpportunityStatus(detail.jobId, actionCode){
                        FlowEventBus.postSticky(Events.OpportunityListRefreshEvent(2))
                    }
                }
            }
            setTextColor(
                ContextCompat.getColor(
                    this@OpportunityDetailActivity,
                    if (state == OpportunityDetailActionState.CLOSED) {
                        R.color.color_55_3c3c43
                    } else {
                        R.color.color_2f6bff
                    }
                )
            )
            setBackgroundResource(
                if (state == OpportunityDetailActionState.CLOSED) {
                    R.drawable.bg_opportunity_detail_closed_button
                } else {
                    R.drawable.bg_opportunity_detail_secondary_button
                }
            )
        }
        binding.tvCommunicateNow.apply {
            text = secondAction?.buttonText.orEmpty()
            isEnabled = secondAction?.actionCode != null
            visibility = if (showSecondAction) View.VISIBLE else View.GONE
            setOnSingleClickListener {
                secondAction?.actionCode?.let { actionCode ->
                    submitBottomAction(detail, actionCode)
                }
            }
        }
    }

    /**
     * 提交底部操作。
     *
     * MARK_COMMUNICATING 先请求创建企业 IM 会话，返回会话信息后进入聊天；
     * 其他操作直接请求更新机会状态，并通知列表刷新。
     */
    private fun submitBottomAction(detail: OpportunityDetailData, actionCode: String) {
        if (actionCode != ACTION_MARK_COMMUNICATING) {
            mRealVM.updateOpportunityStatus(detail.jobId, actionCode) {
                FlowEventBus.postSticky(Events.OpportunityListRefreshEvent(2))
            }
            return
        }
        mRealVM.createEnterpriseImSession(detail.jobId) { session ->
            MessageChatActivity.start(
                this,
                MessageChatParams(
                    sessionId = session.sessionId ?: "",
                    contactId = session.targetImAccId.orEmpty(),
                    name = session.targetNickname.orEmpty(),
                    avatar = session.targetAvatarUrl,
                    currentJobId = session.currentJobId,
                    currentJobName = detail.jobTitle,
                    isFirstCreate = session.created == true,
                    isJobSwitched = session.jobSwitched == true,
                )
            )
        }
    }

    /** 岗位关闭优先级最高，其余状态使用服务端 status 映射。 */
    private fun resolveActionState(
        detail: OpportunityDetailData
    ): OpportunityDetailActionState? = when {
        detail.jobClosed -> OpportunityDetailActionState.CLOSED
        detail.status == "NEW" -> OpportunityDetailActionState.NEW_OPPORTUNITY
        detail.status == "COMMUNICATING" -> OpportunityDetailActionState.COMMUNICATING
        detail.status == "UNSUITABLE" -> OpportunityDetailActionState.UNSUITABLE
        else -> null
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }
}
