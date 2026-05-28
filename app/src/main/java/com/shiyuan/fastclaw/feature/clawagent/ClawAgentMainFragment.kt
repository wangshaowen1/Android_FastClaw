package com.shiyuan.fastclaw.feature.clawagent

import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ScreenUtils
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.common.utils.RecyclerViewScrollHelper
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.AgentMsgAction
import com.shiyuan.fastclaw.core.network.apis.response.AttributeConfirmPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.HistoryResumeConfirmPayloadBean
import com.shiyuan.fastclaw.databinding.FragmentClawAgentMainBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.feature.mine.ShrimpUnderstandsActivity
import com.shiyuan.fastclaw.feature.report.UploadResumeDialog
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
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
                if (mRealVM.streaming.value == true) {
                    mRealVM.cancelStream(flag = false)
                }
                // 主动发送:本条乐观插入及随后的流式回复都强制滚到最新
                forceScrollToBottom = true
                mRealVM.sendMessage(content.toString().trim())
                binding.etSend.text = null
            }
            R.id.tv_agent_memory -> {
                if(!AppNavigator.checkLoginIntercepted(false)){
                    ShrimpUnderstandsActivity.start(requireActivity())
                }
            }

        }
    }

    private fun initView() {
        // 消息列表：聊天场景内容自底部对齐
        binding.rvMsg.layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.rvMsg.addItemDecoration(SpaceDecoration(12.dp, SpaceDecoration.Direction.TOP_WITH_BOTTOM))
        binding.rvMsg.adapter = msgAdapter

        msgAdapter.onUploadClickAction = { data ->
            showUploadResumeDialog(data)
        }
        // 补问卡选项点击：actionCode 取自 payload option（如 QUESTION_OPTION_SUBMIT），actionPayload 原样回传
        msgAdapter.onQuestionOptionClickAction = { item, option, queueId ->
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
        // 补问卡动作按钮（如跳过）：actionCode 取自 payload action
        msgAdapter.onQuestionActionClickAction = { item, action, queueId ->
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
        // 历史简历确认卡 确认/否认：actionCode 用端侧动作枚举，回传 resumeId/resumeStructId/resumeTaskId
        msgAdapter.onResumeConfirmClickAction = { item, confirmed ->
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
                } else {
                    binding.ivSend.gone()
                }
            } ?: run {
                binding.ivSend.gone()
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.etSend) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (ime.bottom <= 0) {
                    bottomMargin = 12.dp
                } else {
                    bottomMargin = ime.bottom - 90.dp
                }
            }
            insets
        }
    }

    private fun observeData() {
        observeEvents {
            on<Events.UserChangedEvent> { event ->
                // 用户操作重置
                mRealVM.reset()
                if (event.eventType == Events.UserChangedEvent.SIGN_IN) {
                    // 登录重新建立会话
                    if (!User.get().isVisitor) {
                        mRealVM.enterSession()
                    }
                }
            }
        }

        if (!User.get().isVisitor) {
            mRealVM.enterSession()
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
        }
        // 收到属性确认卡：弹出记忆确认弹窗（一次性事件，消费后置空防重入）
        mRealVM.attributeConfirmPopup.observe(viewLifecycleOwner) { info ->
            info ?: return@observe
            showMemoryCardPopup(info)
            mRealVM.attributeConfirmPopup.value = null
        }
    }

    private fun showUploadResumeDialog(data: ClawAgentMessageInfo) {
        UploadResumeDialog.show(manager = this.parentFragmentManager) { fileInfo ->
            if (fileInfo != null) {
                mRealVM.submitResume(fileInfo.uri) { path ->
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

    // 记忆卡片
    private var popup: PopupWindow? = null

    /** 记忆卡片弹出后自动确认的延时任务（手动点击或关闭时取消，避免重复触发） */
    private var autoConfirmJob: Job? = null

    private fun showMemoryCardPopup(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, AttributeConfirmPayloadBean::class.java) }.getOrNull()
        }
        val queueId = payload?.questionQueueId
        val contentView = LayoutInflater.from(context).inflate(R.layout.popup_memory_card_select, null, false)
        val contentTv = contentView.findViewById<TextView>(R.id.tv_content)
        val confirmBtn = contentView.findViewById<TextView>(R.id.tv_confirm)
        val negationBtn = contentView.findViewById<TextView>(R.id.tv_negation)
        val jumpMemory = contentView.findViewById<TextView>(R.id.tv_all_memory)

        contentTv.text = payload?.memoryText ?: payload?.questionText ?: item.content
        jumpMemory.setOnSingleClickListener {
            if(!AppNavigator.checkLoginIntercepted(false)){
                ShrimpUnderstandsActivity.start(requireActivity())
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

        // 弹出 1s 后自动触发"确认"事件并关闭弹窗（随 view 生命周期取消）
        autoConfirmJob?.cancel()
        autoConfirmJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(2000L)
            confirmBtn.performClick()
        }
    }

    /** 组装属性确认动作回传 payload：questionQueueId / 分组 / 元属性 / 候选值 + 选择文案 */
    private fun buildAttributeActionPayload(payload: AttributeConfirmPayloadBean?, selectedText: String): String {
        val map = buildMap {
            payload?.questionQueueId?.let { put("questionQueueId", it) }
            payload?.attributeGroupCode?.let { put("attributeGroupCode", it) }
            payload?.metaCode?.let { put("metaCode", it) }
            payload?.candidateValue?.let { put("candidateValue", it) }
            put("selectedText", selectedText)
        }
        return GsonUtils.toJson(map)
    }

    private fun dismissMemoryCard() {
        autoConfirmJob?.cancel()
        autoConfirmJob = null
        popup?.dismiss()
        popup = null
    }
}