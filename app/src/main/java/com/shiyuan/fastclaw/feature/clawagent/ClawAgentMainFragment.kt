package com.shiyuan.fastclaw.feature.clawagent

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.SpanUtils
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.common.Constants.COMPANY_BACKGROUND_RESULT_URL
import com.shiyuan.fastclaw.common.Constants.MOCK_INTERVIEW_RESULT_URL
import com.shiyuan.fastclaw.common.Constants.MOCK_PASS_PACKET_RESULT_URL
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.invisible
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.common.utils.KeyboardUtils
import com.shiyuan.fastclaw.common.utils.KeyboardUtils.hideKeyboard
import com.shiyuan.fastclaw.common.utils.SoftKeyBoardListener
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.network.apis.request.AgentMsgAction
import com.shiyuan.fastclaw.core.network.apis.request.FileUploadCredentialItemBody.Companion.BIZ_TYPE_XIAOXIA_RESUME_UPLOAD
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.MockInterviewStartBody
import com.shiyuan.fastclaw.core.network.apis.response.AttributeConfirmPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.HistoryResumeConfirmPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.SalaryRangePayload
import com.shiyuan.fastclaw.databinding.FragmentClawAgentMainBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentViewModel.Companion.ATTRIBUTE_GROUP_JOB_STATUS
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentViewModel.Companion.ERROR_CODE_MODEL_FAILED
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentViewModel.Companion.ERROR_CODE_NOT_COMPANY
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentViewModel.Companion.ERROR_CODE_POINTS_INSUFFICIENT
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentViewModel.Companion.QUICK_ENTRY
import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.feature.clawagent.dialog.ClawAgentInsufficientShrimpDialog
import com.shiyuan.fastclaw.feature.clawagent.dialog.ClawAgentStatusDialog
import com.shiyuan.fastclaw.feature.information.IndustrySelectionActivity
import com.shiyuan.fastclaw.feature.information.JobCitySelectorActivity
import com.shiyuan.fastclaw.feature.information.JobSelectionActivity
import com.shiyuan.fastclaw.feature.information.JobWelfareSelectorActivity
import com.shiyuan.fastclaw.feature.information.dialog.BirthYearMonthPickerDialog
import com.shiyuan.fastclaw.feature.information.dialog.CertificateSkillsAwardsDialog
import com.shiyuan.fastclaw.feature.information.dialog.EducationExperienceDialog
import com.shiyuan.fastclaw.feature.information.dialog.ProjectExperienceDialog
import com.shiyuan.fastclaw.feature.information.dialog.SalaryRangePickerDialog
import com.shiyuan.fastclaw.feature.information.dialog.WorkExperienceDialog
import com.shiyuan.fastclaw.feature.interview.InterviewActivity
import com.shiyuan.fastclaw.feature.interview.dialog.InterviewRecoverDialog
import com.shiyuan.fastclaw.feature.invite.InviteFriendsActivity
import com.shiyuan.fastclaw.feature.mine.MineShrimpRechargeActivity
import com.shiyuan.fastclaw.feature.mine.OnlineResumeActivity
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.router.AppNavigator
import com.xiaomi.push.co
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/** 
 * @createtime 2026年05月20日 10:25
 * @author TooWhiteT
 */

class ClawAgentMainFragment : BaseClawAgentFragment<FragmentClawAgentMainBinding, ClawAgentViewModel>() {

    private val msgAdapter by lazy { ClawAgentMsgAdapter() }

    /** 用户主动发送后,强制把列表滚到最新一条(不受当前是否已在底部影响) */
    private var forceScrollToBottom = false

    private var hasObservedNonEmptyMessageList = false

    /** IME 动画进行中flag */
    private var hasRunningImeAnimation = false

    private var mCurrentMessage: ClawAgentMessageInfo? = null

    private var mCurrentAction: CardAction? = null

    private var mCurrentQuestionId: String? = null

    private var resumeEventPullJob: Job? = null

    private var isVoiceInputMode = false

    private var agentStatusPopup: ClawAgentStatusDialog? = null

    private var mJobStatusSelected: String? = null

    private var isReplyAnswer = false

    companion object {
        const val TAG = "ClawAgentMainFragment"
        /** 记忆卡片等待时长 */
        private const val MEMORY_CARD_WAIT_TIME = 5000L

        /** 距顶部多少条时开始预取更早历史 */
        private const val PREFETCH_THRESHOLD = 5
        private const val KEYBOARD_BASE_BOTTOM_OFFSET = 95
        private const val RESUME_EVENT_POLL_INTERVAL = 10000L
        private const val RESUME_EVENT_POLL_TIMEOUT = 60000L

        fun getInstance(): ClawAgentMainFragment {
            return ClawAgentMainFragment()
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        observeData()
    }

    // 调用拉取增量消息
    override fun onResume() {
        super.onResume()
        if (!User.get().isVisitor) {
            if (mRealVM.hasInitSession.value == true) {
                mRealVM.pullMessages(mRealVM.getLastMessageCursor())
            }
        }
        if (!User.get().isVisitor) {
            mRealVM.queryUnfinishedInterview()
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.iv_send -> { //发送消息
                if (User.get().isVisitor) {
                    showMessage("请先登录")
                    return
                }
                val content = binding.etSend.text
                if (content.isBlank()) {
                    showMessage("发送内容不能为空")
                    return
                }
                // SSE 仍在 done / 断开 / 关闭之前（streaming=true），拦截发送，不打断当前流
                if (interceptIfStreaming()) return
                // 主动发送:本条乐观插入及随后的流式回复都强制滚到最新
                forceScrollToBottom = true
                binding.etSend.text = null
                if (isReplyAnswer) {
                    startMsgAction(mCurrentMessage, mCurrentAction, mCurrentQuestionId, text = content.toString().trim())
                    binding.ivReanswerClose.performClick()
                } else {
                    mRealVM.sendMessage(content.toString().trim())
                }
            }
            R.id.iv_agent_memory -> { //对你的理解
                if (!AppNavigator.checkLoginIntercepted(false)){
                    OnlineResumeActivity.start(requireActivity(),0)
                }
            }
            R.id.layout_back_check -> { //背景调查
                if (!AppNavigator.checkLoginIntercepted(false) && !interceptIfStreaming()) {
                    forceScrollToBottom = true
                    mRealVM.sendAction(
                        content = getString(R.string.company_backcheck),
                        actionData = AgentMsgAction(
                            actionCode = CandidateJobAgentClientActionEnum.OPEN_COMPANY_BACKGROUND.name,
                            actionPayloadJson = null,
                        )
                    )
                }
            }
            R.id.layout_interview -> { //模拟面试
                if (!AppNavigator.checkLoginIntercepted(false) && !interceptIfStreaming()) {
                    forceScrollToBottom = true
                    mRealVM.sendAction(
                        content = getString(R.string.mock_interview),
                        actionData = AgentMsgAction(
                            actionCode = CandidateJobAgentClientActionEnum.MOCK_INTERVIEW_QUICK_ENTRY.name,
                            actionPayloadJson = GsonUtils.toJson(MockInterviewStartBody(entrySource = QUICK_ENTRY))
                        )
                    )
                }
            }
            R.id.layout_pass -> { //面试通关包
                if(!AppNavigator.checkLoginIntercepted(false) && !interceptIfStreaming()) {
                    forceScrollToBottom = true
                    mRealVM.queryInterviewPassPackageResumeStatus()
                }
             }
            R.id.iv_scroll_bottom -> { //滚动到底部
                scrollToBottom(true, 0L)
            }
            R.id.iv_reanswer_close -> { //重新回答关闭
                isReplyAnswer = false
                binding.layoutReanswer.gone()
                binding.layoutSend.setCorners(27f)
                binding.etSend.text = null
                hideKeyboard(binding.etSend)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        // 消息列表：聊天场景内容自底部对齐
        binding.rvMsg.layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.rvMsg.itemAnimator = null
        // 列表 item 统一留 12dp 间距，首尾额外撑开顶部和输入框之间的距离。
        binding.rvMsg.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: android.graphics.Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (position == RecyclerView.NO_POSITION) return
                val itemCount = state.itemCount
                outRect.set(
                    0,
                    if (position == 0) 12.dp else 0,
                    0,
                    if (position == itemCount - 1) 21.dp else 12.dp
                )
            }
        })
        binding.rvMsg.adapter = msgAdapter

        binding.tvAgentDesc.setOnSingleClickListener {
            showAgentStatusDialog()
        }
        binding.tvAgentIndex.setOnSingleClickListener {
            showAgentStatusDialog()
        }

        msgAdapter.onUploadClickAction = { data,reUpload ->
            if (!AppNavigator.checkLoginIntercepted(false)) {
                // 上传/重传埋点统一收敛到 onTrackingEvent
                onTrackingEvent(trackingType = if (reUpload) TrackingType.UPLOAD_RESUME_RETRY else TrackingType.UPLOAD_RESUME)
                showUploadResumeDialog(data)
            }
        }

        // 历史简历确认卡 确认/否认：actionCode 用端侧动作枚举，回传 resumeId/resumeStructId/resumeTaskId
        msgAdapter.onResumeConfirmClickAction = { item, confirmed ->
            if (!interceptIfStreaming()) {
                // 确认/否认埋点统一收敛到 onTrackingEvent，原有 forceScrollToBottom 判定保留
                onTrackingEvent(
                    trackingType = if (forceScrollToBottom) TrackingType.RESUME_CONFIRM else TrackingType.RESUME_DENY
                )
                forceScrollToBottom = true
                val payload = item.metaData?.let {
                    runCatching { GsonUtils.fromJson(it, HistoryResumeConfirmPayloadBean::class.java) }.getOrNull()
                }
                val payloadMap = buildMap {
                    payload?.resumeId?.let { put("resumeId", it) }
                    payload?.resumeStructId?.let { put("resumeStructId", it) }
                    payload?.resumeTaskId?.let { put("resumeTaskId", it) }
                }
                mRealVM.sendAction(
                    content = getString(if (confirmed) R.string.confirm_my_resume else R.string.not_my_resume),
                    actionData = AgentMsgAction(
                        actionCode = (if (confirmed) CandidateJobAgentClientActionEnum.RESUME_CONFIRM_YES
                        else CandidateJobAgentClientActionEnum.RESUME_CONFIRM_NO).name,
                        sourceMessageId = item.id,
                        actionPayloadJson = payloadMap.takeIf { it.isNotEmpty() }?.let { GsonUtils.toJson(it) },
                    )
                )
            }
        }
        // 公司背调 - 开始背调
        msgAdapter.onCompanyStartClickAction = { _, _, data ->
            if (!interceptIfStreaming()) {
                forceScrollToBottom = true
                mRealVM.submitCompanyBackgroundPreCheck(data.companyId)
            }
        }
        // 公司背调 - 搜索公司
        msgAdapter.onCompanySearchQueryAction = { keyword, callback ->
            mRealVM.queryCompanyBackgroundCompanySearch(keyword, callback)
        }
        // 公司背调 - 查看报告
        msgAdapter.onCompanyDoneClickAction = { _, _, data ->
            if (!interceptIfStreaming()) {
                SimpleBrowserActivity.launch(requireActivity(), COMPANY_BACKGROUND_RESULT_URL + data?.reportId)
            }
        }
        // 公司背调 - 重新生成
        msgAdapter.onCompanyRetryClickAction = { _, _, data ->
            if (!interceptIfStreaming()) {
                forceScrollToBottom = true
                mRealVM.retryGenerateCompanyBackgroundReport(data?.reportId)
            }
        }
        // 模拟面试 - 开始面试
        msgAdapter.onInterviewStartClickAction = { item, action, data ->
            if (!interceptIfStreaming()) {
                forceScrollToBottom = true
                mRealVM.startInterview(data?.entryMessageId, data?.entrySource, data?.resumeId)
            }
        }
        // 模拟面试 - 暂不需要
        msgAdapter.onInterviewNotNeedClickAction = { item, action, data ->
            if (!interceptIfStreaming()) {
                forceScrollToBottom = true
                mRealVM.sendAction(
                    content = action?.text.orEmpty(),
                    actionData = AgentMsgAction(
                        actionCode = action?.actionCode,
                        sourceMessageId = item.id,
                        actionPayloadJson = action?.actionPayload?.let { GsonUtils.toJson(it) },
                    )
                )
            }
        }
        // 模拟面试报告 - 面试报告
        msgAdapter.onInterviewDoneClickAction = { item, action, data ->
            if (!interceptIfStreaming()) {
                SimpleBrowserActivity.launch(
                    requireActivity(),
                    MOCK_INTERVIEW_RESULT_URL + data?.interviewId + "&interview_mrId=" + data?.mockReportId
                )
            }
        }
        // 模拟面试报告 - 重新生成
        msgAdapter.onInterviewRetryClickAction = { _, _, data ->
            if (!interceptIfStreaming()) {
                forceScrollToBottom = true
                mRealVM.retryGenerateMockInterviewReport(data?.mockReportId, data?.interviewId)
            }
        }
        // 面试通关包 - 查看报告
        msgAdapter.onPassPackageDoneClickAction = { item, action, data ->
            if (!interceptIfStreaming()) {
                SimpleBrowserActivity.launch(
                    requireActivity(),
                    MOCK_PASS_PACKET_RESULT_URL + data?.packageId
                )
            }
        }
        // 面试通关包 - 重新生成
        msgAdapter.onPassPackageRetryClickAction = { _, _, data ->
            if (!interceptIfStreaming()) {
                forceScrollToBottom = true
                mRealVM.retryGeneratePassPackageReport(data?.packageId)
            }
        }

        // 补问卡动作按钮（如跳过）：actionCode 取自 payload action
        msgAdapter.onDefaultActionClickAction = { item, action, queueId ->
            if (!interceptIfStreaming()) {
                forceScrollToBottom = true
                mRealVM.sendAction(
                    content = action.text.orEmpty(),
                    actionData = AgentMsgAction(
                        questionQueueId = queueId,
                        actionCode = action.actionCode,
                        sourceMessageId = item.id,
                        actionPayloadJson = action.actionPayload?.let { GsonUtils.toJson(it) },
                    )
                )
            }
        }

        // 补问卡选项点击：actionCode 取自 payload option（如 QUESTION_OPTION_SUBMIT），actionPayload 原样回传
        msgAdapter.onQuestionOptionClickAction = { item, option, queueId ->
            if (!interceptIfStreaming()) {
                val payload = item.metaData?.let {
                    runCatching { GsonUtils.fromJson(it, CoreQuestionPayloadBean::class.java) }.getOrNull()
                }
                forceScrollToBottom = true
                mRealVM.sendAction(
                    content = option.text ?: option.label.orEmpty(),
                    actionData = AgentMsgAction(
                        questionQueueId = queueId,
                        actionCode = option.actionCode,
                        sourceMessageId = item.id,
                        actionPayloadJson = option.actionPayload?.let { GsonUtils.toJson(it) },
                    )
                )
                if (payload?.isJobStatus() ?: false) {// 求职状态
                    mJobStatusSelected = option.text
                    mRealVM.queryJobStatus(ATTRIBUTE_GROUP_JOB_STATUS)
                }
                onTrackingEvent(payload  = payload)
            }
        }
        // 文本补问卡点击“重新回答”时，聚焦底部输入框并拉起输入法
        msgAdapter.onQuestionInputClickAction = { item, action, queueId ->
            if (!interceptIfStreaming()) {
                mCurrentMessage = item
                mCurrentAction = action
                mCurrentQuestionId = queueId
                isReplyAnswer = true
                isVoiceInputMode = false
                binding.layoutReanswer.visible()
                binding.layoutSend.setCorners(20f)
                binding.etSend.setText(action.actionPayload?.skipBubbleText?:"")
                binding.etSend.setSelection(binding.etSend.text.length)
                updateInputModeView()
                binding.etSend.post {
                    binding.etSend.requestFocus()
                    binding.etSend.setSelection(binding.etSend.text?.length ?: 0)
                    KeyboardUtils.showKeyboard(binding.etSend)
                }
            }
        }
        // 补问卡动作按钮：actionCode 取自 payload action
        msgAdapter.onQuestionActionClickAction = { item, action, queueId ->
            if (!interceptIfStreaming()) {
                val payload = item.metaData?.let {
                    mCurrentMessage = item
                    mCurrentAction = action
                    mCurrentQuestionId = queueId
                    runCatching { GsonUtils.fromJson(it, CoreQuestionPayloadBean::class.java) }.getOrNull()
                }
                if (payload?.isLocationCity() ?: false) {// 现居住城市
                    JobCitySelectorActivity.start(activity, JobCityQueryBody.LOCATION)
                } else if (payload?.isExpectedCity() ?: false) { //意向城市
                    JobCitySelectorActivity.start(activity, JobCityQueryBody.EXPECTED_CITY)
                } else if (payload?.isJobIntention() ?: false) { //意向岗位
                    JobSelectionActivity.start(activity,true,mRealVM.sessionId,mRealVM.conversationId, multipleNum = 10)
                } else if (payload?.isExpectedIndustry() ?: false) { //期望行业
                    IndustrySelectionActivity.start(activity, true, mRealVM.sessionId,mRealVM.conversationId)
                }  else if (payload?.isWelfarePreference() ?: false) { //福利偏好
                    JobWelfareSelectorActivity.start(activity, mRealVM.sessionId, mRealVM.conversationId)
                } else if (payload?.isBirthday() ?: false) { //出生年月
                    BirthYearMonthPickerDialog.show(
                        manager = childFragmentManager,
                        maxTimestamp = 1767143497521,
                        title = "出生年月",
                        onConfirmWithTimestamp = { year, month, _ ->
                            FlowEventBus.postSticky(Events.ClawAgentEvent("$year-$month"))
                        }
                    )
                } else if (payload?.isExpectedSalary() ?: false) { //薪资范围
                    SalaryRangePickerDialog.show(
                        manager = childFragmentManager
                    ) { minSalary, maxSalary ->
                        val salaryPayloadJson = SalaryRangePayload.fromPicker(minSalary, maxSalary).toJson()
                        FlowEventBus.postSticky(Events.ClawAgentEvent(salaryPayloadJson,AppEvent.KLX_C_EXPECTED_SALARY_COMPLETED))
                    }
                } else if (payload?.isWorkExperience() ?: false) { //工作经历
                    if(mRealVM.currentRequestId.isNullOrEmpty()){
                        mRealVM.currentRequestId = UUID.randomUUID().toString()
                    }
                    WorkExperienceDialog.show(
                        manager = childFragmentManager,
                        questionQueueId = queueId,
                        attributeGroupCode = payload.attributeGroupCode,
                        clientRequestId = mRealVM.currentRequestId,
                        sessionId = mRealVM.sessionId,
                        conversationId = mRealVM.conversationId,
                        onCompleteClick = { ->
                            FlowEventBus.postSticky(Events.ClawAgentEvent(null))
                        }
                    )
                } else if (payload?.isEducationInfo() ?: false) { //教育经历
                    if(mRealVM.currentRequestId.isNullOrEmpty()){
                        mRealVM.currentRequestId = UUID.randomUUID().toString()
                    }
                    EducationExperienceDialog.show(
                        manager = childFragmentManager,
                        questionQueueId = queueId,
                        attributeGroupCode = payload.attributeGroupCode,
                        clientRequestId = mRealVM.currentRequestId,
                        sessionId = mRealVM.sessionId,
                        conversationId = mRealVM.conversationId,
                        onCompleteClick = { ->
                            FlowEventBus.postSticky(Events.ClawAgentEvent(null))
                        }
                    )
                } else if (payload?.isProjectExperience() ?: false) { //项目经历
                    if(mRealVM.currentRequestId.isNullOrEmpty()){
                        mRealVM.currentRequestId = UUID.randomUUID().toString()
                    }
                    ProjectExperienceDialog.show(
                        manager = childFragmentManager,
                        questionQueueId = queueId,
                        attributeGroupCode = payload.attributeGroupCode,
                        clientRequestId = mRealVM.currentRequestId,
                        sessionId = mRealVM.sessionId,
                        conversationId = mRealVM.conversationId,
                        onCompleteClick = { ->
                            FlowEventBus.postSticky(Events.ClawAgentEvent(null))
                        }
                    )
                } else if (payload?.isSkills() ?: false) { //技能
                    if(mRealVM.currentRequestId.isNullOrEmpty()){
                        mRealVM.currentRequestId = UUID.randomUUID().toString()
                    }
                    CertificateSkillsAwardsDialog.show(
                        manager = childFragmentManager,
                        type = payload.attributeGroupCode ?: "",
                        questionQueueId = queueId,
                        attributeGroupCode = payload.attributeGroupCode,
                        clientRequestId = mRealVM.currentRequestId,
                        sessionId = mRealVM.sessionId,
                        conversationId = mRealVM.conversationId,
                        onCompleteClick = { ->
                            FlowEventBus.postSticky(Events.ClawAgentEvent(null))
                        }
                    )
                } else if (payload?.isCertificates() ?: false) { //证书
                    if(mRealVM.currentRequestId.isNullOrEmpty()){
                        mRealVM.currentRequestId = UUID.randomUUID().toString()
                    }
                    CertificateSkillsAwardsDialog.show(
                        manager = childFragmentManager,
                        type = payload.attributeGroupCode ?: "",
                        questionQueueId = queueId,
                        attributeGroupCode = payload.attributeGroupCode,
                        clientRequestId = mRealVM.currentRequestId,
                        sessionId = mRealVM.sessionId,
                        conversationId = mRealVM.conversationId,
                        onCompleteClick = { ->
                            FlowEventBus.postSticky(Events.ClawAgentEvent(null))
                        }
                    )
                } else if (payload?.isAwards() ?: false) { //奖项
                    if(mRealVM.currentRequestId.isNullOrEmpty()){
                        mRealVM.currentRequestId = UUID.randomUUID().toString()
                    }
                    CertificateSkillsAwardsDialog.show(
                        manager = childFragmentManager,
                        type = payload.attributeGroupCode ?: "",
                        questionQueueId = queueId,
                        attributeGroupCode = payload.attributeGroupCode,
                        clientRequestId = mRealVM.currentRequestId,
                        sessionId = mRealVM.sessionId,
                        conversationId = mRealVM.conversationId,
                        onCompleteClick = { ->
                            FlowEventBus.postSticky(Events.ClawAgentEvent(null))
                        }
                    )
                }  else {
                    startMsgAction(item, action, queueId)
                }
                onTrackingEvent(payload = payload)
            }
        }

        // 无感知加载：向上滚动到接近顶部时提前预取更早历史（DiffUtil 顶部静默插入，不出现下拉转圈）
        binding.rvMsg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                updateScrollBottomButton()
                if (dy >= 0) return // 仅在向上滚动时预取
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                if (lm.findFirstVisibleItemPosition() <= PREFETCH_THRESHOLD) {
                    mRealVM.loadOlderHistory()
                }
            }
        })
        binding.rvMsg.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // 点击当前卡片外部空白处，isReplyAnswer=true展开的卡片隐藏/收起来
                val child = binding.rvMsg.findChildViewUnder(event.x, event.y)
                val position = child?.let { binding.rvMsg.getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
                if (msgAdapter.items.getOrNull(position)?.isReplyAnswer != true) {
                    msgAdapter.collapseExpandedItem()
                }
            }
            if (KeyboardUtils.isSoftInputShown(activity?.window)) {
                hideKeyboard(binding.etSend)
            }
            false // 不影响 RecyclerView 正常滑动
        }

        // 下拉加载更早历史消息（顶部下拉）
        binding.srlMsg.setOnRefreshListener {
            // 进入失败，下拉调用进入会话
            if (mRealVM.sessionData.value == null) {
                mRealVM.enterSession()
                return@setOnRefreshListener
            }
//            mRealVM.loadOlderHistory()
        }

        binding.ivAgentAvatar.setOnClickListener {
//            mRealVM.testPopupMsg()
        }

        binding.etSend.doAfterTextChanged {
            updateInputModeView()
        }

        binding.ivInputMode.setOnClickListener {
            isVoiceInputMode = !isVoiceInputMode
            if (isVoiceInputMode) {
                hideKeyboard(binding.etSend)
            }
            updateInputModeView()
        }

        binding.tvVoicePress.setOnTouchListener { _, event ->
            handleVoicePressTouch(event)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutSend) { v, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigationBottom = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom
            // 动画期间不再改变margin，防多次分发触发动画
            if (!hasRunningImeAnimation) {
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = resolveBottomMargin(imeBottom, navigationBottom)
                }
            }
            insets
        }
        // 键盘动画监听期间逐帧跟随 IME，键盘弹/收过程中 layoutSend 跟随改变底部边距
        ViewCompat.setWindowInsetsAnimationCallback(binding.layoutSend, object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        hasRunningImeAnimation = true
                    }
                }

                override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat {
                    val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    val navigationBottom = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom
                    binding.layoutSend.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = resolveBottomMargin(imeBottom, navigationBottom)
                    }
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        hasRunningImeAnimation = false
                    }
                }
            }
        )

        SoftKeyBoardListener(activity).setOnSoftKeyBoardChangeListener(object : SoftKeyBoardListener.OnSoftKeyBoardChangeListener {
            override fun keyBoardShow(height: Int) {
                isScrollBottomKeyboardShowing = true
                binding.layoutAction.gone()
                binding.ivScrollBottom.gone()
            }

            override fun keyBoardHide(height: Int) {
                isScrollBottomKeyboardShowing = false
                updateScrollBottomButton()
                binding.layoutAction.visible()
                // 延迟 100 毫秒后重置动画锁并请求重新分发 Insets
                binding.layoutSend.postDelayed({
                    hasRunningImeAnimation = false
                    ViewCompat.requestApplyInsets(binding.layoutSend)
                }, 250L)
            }
        })
    }

    private fun observeData() {
        observeEvents {
            on<Events.UserChangedEvent> { event ->
                // 用户操作重置
                mRealVM.reset()
                when (event.eventType) {
                    Events.UserChangedEvent.SIGN_IN -> {
                        // 登录重新建立会话
                        if (!User.get().isVisitor) {
                            mRealVM.enterSession()
                        }
                    }
                    Events.UserChangedEvent.SIGN_OUT -> {
                        mRealVM.hardPreMsgListCard(insertPreCard())
                        mRealVM.sessionData.value = mRealVM.sessionData.value?.copy(jobSearchStatus = null) //清除求职状态
                    }
                }
            }
            on<Events.ResumeEvent> { event ->
                // 上传简历通知拉取
                if (User.get().isVisitor) return@on
                if (mRealVM.hasInitSession.value == true) {
                    mRealVM.pullMessages(mRealVM.getLastMessageCursor())
                    resumeEventPullJob?.cancel()
                    resumeEventPullJob = lifecycleScope.launch {
                        val startTime = System.currentTimeMillis()
                        while (!User.get().isVisitor
                            && mRealVM.hasInitSession.value == true
                            && System.currentTimeMillis() - startTime < RESUME_EVENT_POLL_TIMEOUT
                        ) {
                            delay(RESUME_EVENT_POLL_INTERVAL)
                            mRealVM.pullMessages(mRealVM.getLastMessageCursor())
                        }
                    }
                }
            }
            on<Events.ClawAgentEvent> { event ->
                onTrackingEvent(clawAgentEvent = event)
                //聊天对话选择后回传
                startMsgAction(mCurrentMessage, mCurrentAction, mCurrentQuestionId, msgGson = event.msgGson)
            }
            /**
             * 接受消息，点击公司背调和模拟面试按钮
             */
            on<Events.CompanyInterviewEvent> { event ->
                when (event.type){
                    0 ->  binding.layoutBackCheck.performClick()
                    else ->  binding.layoutInterview.performClick()
                }
            }

            /**
             * 接受消息，求职状态
             */
            on<Events.JobSearchStatusEvent> { event ->
                mRealVM.updateJobStatus(event.code)
            }
        }

        if (!User.get().isVisitor) {
            mRealVM.enterSession()
        } else {
            mRealVM.hardPreMsgListCard(insertPreCard())
        }

        mRealVM.messageList.observe(viewLifecycleOwner) { list ->
            // 是否滚到最新：用户刚发送(forceScrollToBottom)强制滚动；否则仅在原本已在底部时
            // 跟随新消息/流式增量，避免打断向上浏览历史。判定须在 submitList 之前取(反映旧位置)。
            val hadObservedNonEmptyMessageList = hasObservedNonEmptyMessageList
            // 用户刚发送消息时强制跟随；否则仅在当前已经贴底时才继续跟随新消息。
            val scrollToLatest = forceScrollToBottom || !binding.rvMsg.canScrollVertically(1)
            // 新增尾部消息才触发滚动；同一条流式内容增量更新不重复滚动。
            val hasNewTailMessage = list.size > msgAdapter.items.size || msgAdapter.items.lastOrNull()?.id != list.lastOrNull()?.id
            // 用提交新列表前的可见位置估算距离底部还有多少个 item。距离底部不超过 5 个 item 时平滑滚动；更远时直接贴底，避免长距离慢慢滑。
            val lastVisiblePosition = (binding.rvMsg.layoutManager as? LinearLayoutManager)?.findLastVisibleItemPosition() ?: -1
            val shouldSmoothScroll = hadObservedNonEmptyMessageList && list.lastIndex - lastVisiblePosition <= 5
            // 首次加载、主动发消息或新增尾部消息时才触发滚动；流式更新同一条消息时不重复滚动。
            val shouldRunScrollCommand = !hadObservedNonEmptyMessageList || forceScrollToBottom || hasNewTailMessage
            // 最终是否需要滚到最新：既要当前允许跟随底部，也要本次列表变化确实需要滚动。
            val shouldScrollToLatest = scrollToLatest && list.isNotEmpty() && shouldRunScrollCommand
            forceScrollToBottom = false
            if (shouldScrollToLatest) {
                // submitList 会先触发布局和滚动回调，提前屏蔽按钮可避免新增卡片时短暂闪现。
                suppressScrollBottomButton()
            }
            msgAdapter.submitList(list)
            // 新消息插入后原最后一条不再是尾项，主动刷新 ItemDecoration 避免保留 21dp 底部间距。
            binding.rvMsg.invalidateItemDecorations()
            if (list.isNotEmpty()) {
                hasObservedNonEmptyMessageList = true
            }
            if (shouldScrollToLatest) {
                // 首次加载直接贴底；新增消息用平滑滚动。流式内容更新不重复触发滚动，避免 srlMsg 抖动。
                scrollToBottom(shouldSmoothScroll)
            }
        }
        // 控制下拉刷新，enterSession请求错误 or 数据有误开启下拉刷新，否则关闭
        mRealVM.hasCanRefresh.observe(viewLifecycleOwner) { hasMore ->
            binding.srlMsg.finishRefresh()
            binding.srlMsg.setEnableRefresh(hasMore)
        }
        mRealVM.streamError.observe(viewLifecycleOwner) { e ->
            e ?: return@observe
            if (!canShowStreamErrorToast()) {
                mRealVM.streamError.value = null
                return@observe
            }
            showMessage(e.message ?: "对话出错")
            mRealVM.streamError.value = null
        }
        // 收到属性确认卡：弹出记忆确认弹窗（一次性事件，消费后置空防重入）
        mRealVM.attributeConfirmPopup.observe(viewLifecycleOwner) { info ->
            info ?: return@observe
            showMemoryCardPopup(info)
            mRealVM.attributeConfirmPopup.value = null
        }
        // 达到开启消息通知弹窗
        mRealVM.understandingSummaryDialog.observe(viewLifecycleOwner) { info ->
            info ?: return@observe
            showNotificationSwitchDialog()
            mRealVM.understandingSummaryDialog.value = null
        }
        // 模拟面试查询未
        mRealVM.interviewUnfinishedResult.observe(this) {
            val data = it ?: return@observe
            mRealVM.interviewUnfinishedResult.value = null
            if (data.hasUnfinished == true) {
                showInterviewRecoverDialog(data.interviewId)
            }
        }
        mRealVM.interviewStartResult.observe(this) {
            if (it?.isDeductSuccess() == true) {
                InterviewActivity.start(requireActivity(), it.interviewId)
            } else if(it?.isDeductFailed() == true){
                showInsufficientShrimpDialog()
            } else{
                showMessage(it?.message ?: "模拟面试创建失败")
            }
        }
        //公司背调 - 前置校验失败提示：优先展示后端 errorMessage，兜底使用文档约定 errorCode 对应文案。
        mRealVM.companyBackgroundResult.observe(this) {
            when (it?.errorCode) {
                ERROR_CODE_NOT_COMPANY -> showToast(R.string.str_agent_company_background_error_not_company)
                ERROR_CODE_POINTS_INSUFFICIENT -> showToast(R.string.str_agent_company_background_error_points_insufficient)
                ERROR_CODE_MODEL_FAILED -> showToast(R.string.str_agent_company_background_error_model_failed)
                else -> showToast( R.string.str_agent_company_background_precheck_failed)
            }
            if (it?.errorCode == ERROR_CODE_POINTS_INSUFFICIENT) {
                showInsufficientShrimpDialog()
            }
        }
        // 监听求职状态属性，使用线上简历属性接口结果同步顶部状态展示
        mRealVM.jobStatusResult.observe(this) { data ->
            if (data?.displayValue.isNullOrBlank()) data?.displayValue = mJobStatusSelected
            val statusCode = data?.options.orEmpty().firstOrNull { it.name == data?.displayValue }?.code
            binding.tvAgentDesc.text = data?.displayValue
                ?.takeIf { it.isNotBlank() }
                ?: requireActivity().getString(R.string.str_agent_desc2)
            when (statusCode) {
                "NOT_LOOKING" -> binding.tvAgentStatus.setShapeSolidColor(requireActivity().getColor(R.color.color_ff383c)).setUseShape()
                "AVAILABLE_IMMEDIATELY", "AVAILABLE_WITHIN_MONTH", "OPEN_TO_OPPORTUNITY" -> binding.tvAgentStatus.setShapeSolidColor(requireActivity().getColor(R.color.color_15D173)).setUseShape()
                else -> binding.tvAgentStatus.setShapeSolidColor(requireActivity().getColor(R.color.color_ff8D28)).setUseShape()
            }
        }
        //面试通关包
        mRealVM.interviewPassPackageResumeStatus.observe(this) { data ->
            val url = if (data?.resumeCheckId == null) {
                Constants.PASS_PACKAGE_INDEX_URL
            } else {
                "${Constants.PASS_PACKAGE_INDEX_URL}&resumeCheckId=${data?.resumeCheckId}"
            }
            SimpleBrowserActivity.launch(requireActivity(), url)
        }
    }

    private fun updateInputModeView() {
        val hasText = binding.etSend.text?.isNotBlank() == true
        if (isVoiceInputMode) {
            binding.etSend.visibility = View.INVISIBLE
            binding.tvVoicePress.visible()
            binding.ivIcon.gone()
            binding.ivSend.gone()
            binding.ivInputMode.visible()
            binding.ivInputMode.setImageResource(R.drawable.ic_agent_keyboard_mode)
        } else {
            binding.etSend.visible()
            binding.tvVoicePress.gone()
            binding.ivSend.visibility = if (hasText) View.VISIBLE else View.GONE
            binding.ivInputMode.visibility = if (hasText) View.GONE else View.VISIBLE
            binding.ivIcon.visibility = if (hasText) View.GONE else View.VISIBLE
            binding.ivInputMode.setImageResource(R.drawable.ic_agent_voice_mode)
        }
    }

    override fun onVoiceTextRecognized(text: String) {
        if (interceptIfStreaming()) return
        forceScrollToBottom = true
        mRealVM.sendMessage(text)
    }

    override fun onVoiceRecognizingStateReset() {
        updateInputModeView()
    }

    private fun startMsgAction(item: ClawAgentMessageInfo?, action: CardAction?, queueId: String?, msgGson: String? = null, text: String? = null) {
        if (!interceptIfStreaming()) {
            forceScrollToBottom = true
            action?.actionPayload?.selectedValue = msgGson ?: action.actionPayload.selectedValue
            action?.actionPayload?.selectedText = text ?: action.actionPayload.selectedText
            mRealVM.sendAction(
                content = action?.text.orEmpty(),
                actionData = AgentMsgAction(
                    questionQueueId = queueId,
                    actionCode = action?.actionCode,
                    sourceMessageId = item?.id,
                    actionPayloadJson = action?.actionPayload?.let { GsonUtils.toJson(it) },
                )
            )
        }
    }

    private fun canShowStreamErrorToast(): Boolean {
        return isAdded && !isHidden && view != null
    }

    private fun showInterviewRecoverDialog(interviewId: String?) {
        InterviewRecoverDialog(requireContext()).showDialog().apply {
            onContinueClickListener = {
                InterviewActivity.start(requireActivity(), interviewId, true)
            }
            onFinishClickListener = {
                mRealVM.forceFinish(interviewId ?: "") {
                    it?.message?.showToast()
                }
            }
        }
    }

    private fun showInsufficientShrimpDialog() {
        ClawAgentInsufficientShrimpDialog.show(childFragmentManager).apply {
            onInviteClick = {
                InviteFriendsActivity.start(requireActivity())
            }
            onRechargeClick = {
                MineShrimpRechargeActivity.start(requireActivity())
            }
        }
    }

    /**
     * 插入初始状态
     */
    private fun insertPreCard(): List<ClawAgentMessageInfo> {
        val tempList = listOf<ClawAgentMessageInfo>(
            ClawAgentMessageInfo(id = "-1", messageType = ClawAgentMsgAdapter.TYPE_WELCOME, sendObj = ClawAgentViewModel.SENDER_AGENT, receiveObj = ClawAgentViewModel.SENDER_USER),
            ClawAgentMessageInfo(id = "-2", messageType = ClawAgentMsgAdapter.TYPE_UPLOAD_WAIT, sendObj = ClawAgentViewModel.SENDER_AGENT, receiveObj = ClawAgentViewModel.SENDER_USER),
        )
        return tempList
    }

    private fun showNotificationSwitchDialog() {
        if (Config.get().hasEnableNotification()) {
            return
        }
        val dialog = NotificationsSwitchDialog.show(childFragmentManager)
        dialog.clickListener = { flag ->
            Config.get().switchNotification(flag)
            if (flag) {
                // todo: 检测通知权限
            }
        }
    }

    private fun showUploadResumeDialog(data: ClawAgentMessageInfo) {
        UploadResumeDialog.show(manager = this.parentFragmentManager,false) { fileInfo ->
            if (fileInfo != null) {
                showLoading("上传中")
                mRealVM.uploadFilesWithSecurityCheck(
                    uriList = listOf(fileInfo.uri),
                    bizType = BIZ_TYPE_XIAOXIA_RESUME_UPLOAD,
                    onSuccess = onSuccess@{ successUrls ->
                        hideLoading()
                        // 文件上传完成回调里可能 AI 已开始回复，此时拦截避免新开 SSE 打断当前流
                        if (interceptIfStreaming()) return@onSuccess
                        val map = mapOf<String, String>(
                            "resumeFileUrl" to successUrls.first(),
                            "fileName" to "${fileInfo.fileName}"
                        )
                        // sse 上传简历动作
                        mRealVM.sendAction(
                            content = "这是我的简历",
                            actionData = AgentMsgAction(
                                actionCode = CandidateJobAgentClientActionEnum.RESUME_UPLOAD_SUBMIT.name,
                                sourceMessageId = data.id,
                                actionPayloadJson = GsonUtils.toJson(map)
                            )
                        )
                    },
                    onFailure = { reason ->
                        hideLoading()
                        reason.showToast() },
                )
            }
        }
    }

    /**
     * SSE 仍在 done / 断开 / 关闭之前（streaming=true）时拦截用户交互：
     * 弹 Toast 提示并返回 true 表示已拦截，调用方应直接结束本次操作。
     */
    private fun interceptIfStreaming(): Boolean {
        if (mRealVM.streaming.value == true) {
            showMessage(R.string.str_agent_intercept_hint.getResString())
            return true
        }
        return false
    }

    // 记忆卡片
    private var popup: PopupWindow? = null

    /** 记忆卡片弹出后自动确认的延时任务（手动点击或关闭时取消，避免重复触发） */
    private var autoConfirmJob: Job? = null

    private fun showMemoryCardPopup(item: ClawAgentMessageInfo) {
        dismissMemoryCard()
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, AttributeConfirmPayloadBean::class.java) }.getOrNull()
        }
        val queueId = payload?.questionQueueId
        val contentView = LayoutInflater.from(context).inflate(R.layout.popup_memory_card_select, null, false)
        val contentTv = contentView.findViewById<TextView>(R.id.tv_content)
        val confirmBtn = contentView.findViewById<TextView>(R.id.tv_confirm)
        val negationBtn = contentView.findViewById<TextView>(R.id.tv_negation)
        val jumpMemory = contentView.findViewById<TextView>(R.id.tv_all_memory)

        val spanUtils = SpanUtils.with(contentTv)
        if (payload?.confirmationItems.isNullOrEmpty()) {
            // 单个改动，取外层
            spanUtils.append("${payload?.fieldName}：")
                .append("${payload?.displayText}")
                .setForegroundColor(R.color.color_90_000000.getResColor())
                .setBold()
        } else {
            // 多个改动，取里层
            val confirmationItems = payload.confirmationItems
            val size = confirmationItems.size
            confirmationItems.forEachIndexed { index, actionItem ->
                spanUtils.append("${actionItem.fieldName}：")
                    .append("${actionItem.displayText}")
                    .setForegroundColor(R.color.color_90_000000.getResColor())
                    .setBold()
                // 添加换行
                if (index < size - 1) {
                    spanUtils.append("\n")
                }
            }
        }

        spanUtils.create()

        jumpMemory.setOnSingleClickListener {
            if(!AppNavigator.checkLoginIntercepted(false)){
                OnlineResumeActivity.start(requireActivity(),0)
            }
        }

        // 根据后台消息设置卡片按钮文案
        payload?.actions?.forEach { action ->
            when (action.actionCode) {
                CandidateJobAgentClientActionEnum.ATTRIBUTE_CONFIRM.name -> {
                    confirmBtn.text = "${action.text ?: R.string.str_agent_memory_confirm.getResString()}"
                }
                CandidateJobAgentClientActionEnum.ATTRIBUTE_REJECT.name -> {
                    negationBtn.text = "${action.text ?: R.string.str_agent_memory_negation.getResString()}"
                }
            }
        }

        // 确认：ATTRIBUTE_CONFIRM
        confirmBtn.setOnClickListener {
            forceScrollToBottom = true
            val str = R.string.str_agent_memory_confirm.getResString()
            mRealVM.sendAction(
                content = str,
                actionData = AgentMsgAction(
                    actionCode = CandidateJobAgentClientActionEnum.ATTRIBUTE_CONFIRM.name,
                    sourceMessageId = item.id,
                    questionQueueId = queueId,
                    actionPayloadJson = buildAttributeActionPayload(payload, str),
                )
            )
            dismissMemoryCard()
        }
        // 拒绝：ATTRIBUTE_REJECT
        negationBtn.setOnClickListener {
            forceScrollToBottom = true
            val str = R.string.str_agent_memory_negation.getResString()
            mRealVM.sendAction(
                content = str,
                actionData = AgentMsgAction(
                    actionCode = CandidateJobAgentClientActionEnum.ATTRIBUTE_REJECT.name,
                    sourceMessageId = item.id,
                    questionQueueId = queueId,
                    actionPayloadJson = buildAttributeActionPayload(payload, str),
                )
            )
            dismissMemoryCard()
        }
        contentView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        popup = PopupWindow(context)
        popup?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popup?.contentView = contentView
        popup?.width = ScreenUtils.getScreenWidth()
        popup?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popup?.isFocusable = false
        popup?.isOutsideTouchable = false
        popup?.showAtLocation(binding.root, Gravity.TOP, 0, 0)

        // 弹出 5s 后自动触发"确认"事件并关闭弹窗（随 view 生命周期取消）
        autoConfirmJob?.cancel()
        autoConfirmJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(MEMORY_CARD_WAIT_TIME)
            confirmBtn.performClick()
        }
    }

    /** 组装属性确认动作回传 payload：questionQueueId / 分组 / 元属性 / 候选值 + 选择文案 */
    private fun buildAttributeActionPayload(payload: AttributeConfirmPayloadBean?, selectedText: String): String {
        val map = buildMap {
            payload?.questionQueueId?.let { put("questionQueueId", it) }
            payload?.attributeGroupCode?.let { put("attributeGroupCode", it) }
            payload?.metaCode?.let { put("metaCode", it) }
            payload?.confirmMode?.let { put("confirmMode", it) }
//            payload?.candidateValue?.let { put("candidateValue", it) }
            put("selectedText", selectedText)
        }
        return GsonUtils.toJson(map)
    }

    // 计算 layoutSend 相对父容器底部应有的 margin，兼容三键导航和全屏手势导航。
    private fun resolveBottomMargin(imeBottom: Int, navigationBottom: Int): Int {
        val gestureInsetOffset = (binding.layoutContainer.paddingBottom - navigationBottom).coerceAtLeast(0)
        return maxOf(12.dp, imeBottom - KEYBOARD_BASE_BOTTOM_OFFSET.dp + gestureInsetOffset)
    }

    private fun dismissMemoryCard() {
        autoConfirmJob?.cancel()
        autoConfirmJob = null
        popup?.dismiss()
        popup = null
    }

    /**
     * 展示顶部求职状态弹窗：使用线上简历属性接口返回的 options 渲染列表，
     * displayValue 与 option.name 相等时为选中项。
     */
    private fun showAgentStatusDialog() {
        agentStatusPopup?.dismiss()
        val attributeData = mRealVM.jobStatusResult.value ?: return
        binding.tvAgentIndex.setBackgroundResource(R.drawable.ic_arrow_down_gray_12)
        agentStatusPopup = ClawAgentStatusDialog(requireContext(), attributeData).apply {
            onDismissListener = {
                binding.tvAgentIndex.setBackgroundResource(R.drawable.ic_arrow_right_gray_12)
            }
            onItemClickListener = { item ->
                mRealVM.updateJobStatus(item.code)
            }
        }.showDialog(binding.tvAgentDesc)
    }

    override fun onDestroyView() {
        resumeEventPullJob?.cancel()
        resumeEventPullJob = null
        agentStatusPopup?.dismiss()
        agentStatusPopup = null
        super.onDestroyView()
    }

}
