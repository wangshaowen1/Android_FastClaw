package com.shiyuan.fastclaw.feature.clawagent

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
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
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.common.utils.KeyboardUtils
import com.shiyuan.fastclaw.common.utils.KeyboardUtils.hideKeyboard
import com.shiyuan.fastclaw.common.utils.RecyclerViewScrollHelper
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.network.apis.request.AgentMsgAction
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import com.shiyuan.fastclaw.core.network.apis.response.AttributeConfirmPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CoreQuestionPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.HistoryResumeConfirmPayloadBean
import com.shiyuan.fastclaw.databinding.FragmentClawAgentMainBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.feature.information.JobCitySelectorActivity
import com.shiyuan.fastclaw.feature.information.JobSelectionActivity
import com.shiyuan.fastclaw.feature.information.dialog.BirthYearMonthPickerDialog
import com.shiyuan.fastclaw.feature.mine.ShrimpUnderstandsActivity
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.router.AppNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 
 * @createtime 2026年05月20日 10:25
 * @author TooWhiteT
 */

class ClawAgentMainFragment : BaseViewModelFragment<FragmentClawAgentMainBinding, ClawAgentViewModel>() {

    companion object {
        const val TAG = "ClawAgentMainFragment"
        /** 记忆卡片等待时长 */
        private const val MEMORY_CARD_WAIT_TIME = 5000L

        /** 距顶部多少条时开始预取更早历史 */
        private const val PREFETCH_THRESHOLD = 5

        fun getInstance(): ClawAgentMainFragment {
            val fragment = ClawAgentMainFragment()
//            fragment.arguments = Bundle().also {
//
//            }
            return fragment
        }
    }

    private val msgAdapter by lazy { ClawAgentMsgAdapter() }

    /** 用户主动发送后,强制把列表滚到最新一条(不受当前是否已在底部影响) */
    private var forceScrollToBottom = false

    /** IME 动画进行中flag */
    private var hasRunningImeAnimation = false

    private var mCurrentMessage: ClawAgentMessageInfo? = null

    private var mCurrentAction: CardAction? = null

    private var mCurrentQuestionId: String? = null

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
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.iv_send -> {
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
                mRealVM.sendMessage(content.toString().trim())
                binding.etSend.text = null
            }
            R.id.iv_agent_memory -> {
                if(!AppNavigator.checkLoginIntercepted(false)){
                    ShrimpUnderstandsActivity.start(requireActivity())
                }
            }

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        // 消息列表：聊天场景内容自底部对齐
        binding.rvMsg.layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        // 底部间距高点
        binding.rvMsg.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: android.graphics.Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val position = parent.getChildAdapterPosition(view)
                if (position == 0) outRect.top = 12.dp
                val itemCount = state.itemCount
                outRect.bottom = if (position == itemCount - 1) 21.dp else 12.dp
            }
        })
        binding.rvMsg.adapter = msgAdapter

        msgAdapter.onUploadClickAction = { data,reUpload ->
            if (!AppNavigator.checkLoginIntercepted(false)) {
                if(reUpload){
                    EventManager.onAppTrack(AppEvent.KLX_CAPP_UPLOAD_FAILED_RE_RESUME)
                }else{
                    EventManager.onAppTrack(AppEvent.KLX_C_UPLOAD_RESUME,mutableMapOf("klx_pipe_source" to "APP"))
                }
                showUploadResumeDialog(data)
            }
        }
        // 补问卡选项点击：actionCode 取自 payload option（如 QUESTION_OPTION_SUBMIT），actionPayload 原样回传
        msgAdapter.onQuestionOptionClickAction = { item, option, queueId ->
            if (!interceptIfStreaming()) {
                forceScrollToBottom = true
                mRealVM.sendAction(
                    desc = option.text ?: option.label.orEmpty(),
                    actionData = AgentMsgAction(
                        questionQueueId = queueId,
                        actionCode = option.actionCode,
                        sourceMessageId = item.id,
                        actionPayloadJson = option.actionPayload?.let { GsonUtils.toJson(it) },
                    )
                )
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
                    JobSelectionActivity.start(activity,true)
                } else if (payload?.isExpectedIndustry() ?: false) { //期望行业

                } else if (payload?.isLanguageSkills() ?: false) { //语言能力

                } else if (payload?.isBirthday() ?: false) { //出生年月
                    BirthYearMonthPickerDialog.show(childFragmentManager){ year,month->
                        FlowEventBus.postSticky(Events.ClawAgentEvent("$year-$month"))
                    }
                } else if (payload?.isExpectedSalary() ?: false) { //薪资范围

                } else if (payload?.isWorkExperience() ?: false) { //工作经历

                } else if (payload?.isEducationInfo() ?: false) { //教育经历

                } else if (payload?.isSkills() ?: false) { //技能

                } else if (payload?.isProjectExperience() ?: false) { //项目经历

                } else if (payload?.isCertificates() ?: false) { //证书

                } else {
                    startMsgAction(item, action, queueId)
                }
            }
        }
        // 补问卡动作按钮（如跳过）：actionCode 取自 payload action
        msgAdapter.onQuestionSkipClickAction = { item, action, queueId ->
            if (!interceptIfStreaming()) {
                forceScrollToBottom = true
                mRealVM.sendAction(
                    desc = action.text.orEmpty(),
                    actionData = AgentMsgAction(
                        questionQueueId = queueId,
                        actionCode = action.actionCode,
                        sourceMessageId = item.id,
                        actionPayloadJson = action.actionPayload?.let { GsonUtils.toJson(it) },
                    )
                )
            }
        }
        // 历史简历确认卡 确认/否认：actionCode 用端侧动作枚举，回传 resumeId/resumeStructId/resumeTaskId
        msgAdapter.onResumeConfirmClickAction = { item, confirmed ->
            if (!interceptIfStreaming()) {
                EventManager.onAppTrack(if(forceScrollToBottom) AppEvent.KLX_CAPP_CONFIRM_RESUME else AppEvent.KLX_CAPP_DENY_RESUME )
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
                    desc = getString(if (confirmed) R.string.confirm_my_resume else R.string.not_my_resume),
                    actionData = AgentMsgAction(
                        actionCode = (if (confirmed) CandidateJobAgentClientActionEnum.RESUME_CONFIRM_YES
                        else CandidateJobAgentClientActionEnum.RESUME_CONFIRM_NO).name,
                        sourceMessageId = item.id,
                        actionPayloadJson = payloadMap.takeIf { it.isNotEmpty() }?.let { GsonUtils.toJson(it) },
                    )
                )
            }
        }

        // 无感知加载：向上滚动到接近顶部时提前预取更早历史（DiffUtil 顶部静默插入，不出现下拉转圈）
        binding.rvMsg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy >= 0) return // 仅在向上滚动时预取
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                if (lm.findFirstVisibleItemPosition() <= PREFETCH_THRESHOLD) {
                    mRealVM.loadOlderHistory()
                }
            }
        })
        binding.rvMsg.setOnTouchListener { _, _ ->
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

        binding.etSend.doAfterTextChanged { editable ->
            editable?.let {
                if (it.isNotEmpty()) {
                    binding.ivSend.visible()
                    binding.ivIcon.gone()
                } else {
                    binding.ivIcon.visible()
                    binding.ivSend.gone()
                }
            } ?: run {
                binding.ivIcon.visible()
                binding.ivSend.gone()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutSend) { v, insets ->
            // 动画期间不再改变margin，防多次分发触发动画
            if (!hasRunningImeAnimation) {
                val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = resolveBottomMargin(imeBottom)
                }
            }
            insets
        }
        // 键盘动画监听期间逐帧跟随 IME，键盘弹/收过程中 layoutSend 跟随改变底部边距
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.layoutSend,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        hasRunningImeAnimation = true
                    }
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    binding.layoutSend.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = resolveBottomMargin(imeBottom)
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
    }

    private fun startMsgAction(item: ClawAgentMessageInfo?, action: CardAction?, queueId: String?, msgGson: String? = null) {
        if (!interceptIfStreaming()) {
            forceScrollToBottom = true
            action?.actionPayload?.selectedValue = msgGson
            mRealVM.sendAction(
                desc = action?.text.orEmpty(),
                actionData = AgentMsgAction(
                    questionQueueId = queueId,
                    actionCode = "QUESTION_OPTION_SUBMIT",
                    sourceMessageId = item?.id,
                    actionPayloadJson = action?.actionPayload?.let { GsonUtils.toJson(it) },
                )
            )
        }
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
                    }
                }
            }
            on<Events.ResumeEvent> { event ->
                // 上传简历通知拉取
                if (!User.get().isVisitor) {
                    if (mRealVM.hasInitSession.value == true) {
                        mRealVM.pullMessages(mRealVM.getLastMessageCursor())
                    }
                }
            }
            on<Events.ClawAgentEvent> { event ->
                //聊天对话选择后回传
                startMsgAction(mCurrentMessage, mCurrentAction, mCurrentQuestionId, event.msgGson)
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
            val scrollToLatest = forceScrollToBottom || !binding.rvMsg.canScrollVertically(1)
            forceScrollToBottom = false
            msgAdapter.submitList(list)
            if (scrollToLatest && list.isNotEmpty()) {
                // 跳转 + 补偿滚动距离，确保刚插入(尚未测量高度)的最后一条完整贴底
                RecyclerViewScrollHelper.scrollToBottom(binding.rvMsg, 0)
            }
        }
        // 控制下拉刷新，enterSession请求错误 or 数据有误开启下拉刷新，否则关闭
        mRealVM.hasCanRefresh.observe(viewLifecycleOwner) { hasMore ->
            binding.srlMsg.finishRefresh()
            binding.srlMsg.setEnableRefresh(hasMore)
        }
        mRealVM.streamError.observe(viewLifecycleOwner) { e ->
            e ?: return@observe
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
        UploadResumeDialog.show(manager = this.parentFragmentManager) { fileInfo ->
            if (fileInfo != null) {
                mRealVM.submitResume(fileInfo.uri) { path ->
                    // 文件上传完成回调里可能 AI 已开始回复，此时拦截避免新开 SSE 打断当前流
                    if (interceptIfStreaming()) return@submitResume
                    val map = mapOf<String, String>(
                        "resumeFileUrl" to path,
                        "fileName" to "${fileInfo.fileName}"
                    )
                    // sse 上传简历动作
                    mRealVM.sendAction(
                        desc = "这是我的简历",
                        actionData = AgentMsgAction(
                            actionCode = CandidateJobAgentClientActionEnum.RESUME_UPLOAD_SUBMIT.name,
                            sourceMessageId = data.id,
                            actionPayloadJson = GsonUtils.toJson(map)
                        )
                    )
                }
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
                ShrimpUnderstandsActivity.start(requireActivity())
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
                desc = str,
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
                desc = str,
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

    // 计算 layoutSend 相对父容器底部应有的 margin
    private fun resolveBottomMargin(imeBottom: Int): Int =
        // 弹出键盘距离底部距离，收起键盘距离底部距离（82dp距离底部导航栏）
        maxOf(12.dp, imeBottom - 82.dp)

    private fun dismissMemoryCard() {
        autoConfirmJob?.cancel()
        autoConfirmJob = null
        popup?.dismiss()
        popup = null
    }
}