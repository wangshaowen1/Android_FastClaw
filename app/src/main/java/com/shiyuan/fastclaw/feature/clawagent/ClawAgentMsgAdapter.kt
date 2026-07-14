package com.shiyuan.fastclaw.feature.clawagent

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseMultiItemAdapter
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundStartBody
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundEntryBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundReportDoneBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundReportFailedBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundReportGeneratingBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentLeftBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewEntryBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewEntryUpgradeBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewReportDoneBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewReportFailedBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMockInterviewReportGeneratingBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentNoJobsFoundBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentPendingConfirmBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentQuestionBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentSearchingJobsBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentThinkBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUnderstandingUpgradeBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadFailedBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadSuccessBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentWelcomeBinding
import com.shiyuan.fastclaw.databinding.ItemMsgUserRightBinding
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CardOption
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundCompanyInfo
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.PassPackageReportPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewEntryPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.MockInterviewReportPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentPassPackageReportDoneBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentPassPackageReportFailedBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentPassPackageReportGeneratingBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentResumeBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentTableFormBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentTextTipBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentCompanyBackgroundEntryViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentCompanyBackgroundReportDoneViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentCompanyBackgroundReportFailedViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentCompanyBackgroundReportGeneratingViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentPassPackageReportDoneViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentPassPackageReportFailedViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentPassPackageReportGeneratingViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentMockInterviewEntryViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentMockInterviewEntryUpgradeViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentMockInterviewReportDoneViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentMockInterviewReportFailedViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentMockInterviewReportGeneratingViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentNoJobsFoundViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentPendingConfirmViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentQuestionViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentResumeViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentSearchingJobsViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentTableFormViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentTextTipViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentThinkViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentUnderstandingUpgradeViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentUploadFailedViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentUploadSuccessViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentWelcomeViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.SendViewHolder
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * agent对话消息适配器
 *
 * 多布局基于 [BaseMultiItemAdapter]，以 [ClawAgentMessageInfo.messageType] 作为 itemType 直接分发；
 * 列表更新走 [submitList]，内部用 [DiffUtil] 按 [ClawAgentMessageInfo.id] 做差量刷新，避免整表重绘。
 *
 * @createtime 2026年05月22日 09:31
 * @author TooWhiteT
 */
class ClawAgentMsgAdapter : BaseMultiItemAdapter<ClawAgentMessageInfo>() {

    private val typewriterDisplayedLengthMap = mutableMapOf<String, Int>()
    private var hasSubmittedNonEmptyList = false

    var onUploadClickAction: (itemData: ClawAgentMessageInfo,reUpload: Boolean) -> Unit = { _, _ -> }

    /** 补问卡选项点击（B 方案：仅暴露，提交后续统一接 sendAction(QUESTION_OPTION_SUBMIT)） */
    var onQuestionOptionClickAction: (item: ClawAgentMessageInfo, option: CardOption, questionQueueId: String?) -> Unit = { _, _, _ -> }

    /** 补问卡动作按钮点击 */
    var onQuestionActionClickAction: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    /**（如跳过）点击 */
    var onDefaultActionClickAction: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    /** 历史简历确认卡 确认(true)/否认(false) 点击（B 方案：仅暴露，提交后续统一接） */
    var onResumeConfirmClickAction: (item: ClawAgentMessageInfo, confirmed: Boolean) -> Unit = { _, _ -> }

    /** 公司背调 - 开始背调 */
    var onCompanyStartClickAction: (item: ClawAgentMessageInfo, action: CardAction?, data: CompanyBackgroundStartBody) -> Unit = { _, _, _ -> }
    /** 公司背调 - 搜索公司 */
    var onCompanySearchQueryAction: (keyword: String, callback: (keyword: String, companies: List<CompanyBackgroundCompanyInfo>) -> Unit, ) -> Unit = { keyword, callback -> callback(keyword, emptyList()) }
    /** 公司背调 - 查看报告 */
    var onCompanyDoneClickAction: (item: ClawAgentMessageInfo, action: CardAction?, data: CompanyBackgroundReportPayloadBean?) -> Unit = { _, _, _ -> }
    /** 公司背调 - 重新生成 */
    var onCompanyRetryClickAction: (item: ClawAgentMessageInfo, action: CardAction?, data: CompanyBackgroundReportPayloadBean?) -> Unit = { _, _, _ -> }

    /** 模拟面试 - 开始面试 */
    var onInterviewStartClickAction: (item: ClawAgentMessageInfo, action: CardAction?, data: MockInterviewEntryPayloadBean?) -> Unit = { _, _, _ -> }
    /** 模拟面试 - 查看报告 */
    var onInterviewDoneClickAction: (item: ClawAgentMessageInfo, action: CardAction?, data: MockInterviewReportPayloadBean?) -> Unit = { _, _, _ -> }
    /** 模拟面试 - 暂不需要 */
    var onInterviewNotNeedClickAction: (item: ClawAgentMessageInfo, action: CardAction?, data: MockInterviewEntryPayloadBean?) -> Unit = { _, _, _ -> }
    /** 模拟面试报告 - 重新生成 */
    var onInterviewRetryClickAction: (item: ClawAgentMessageInfo, action: CardAction?, data: MockInterviewReportPayloadBean?) -> Unit = { _, _, _ -> }
    /** 面试通关包 - 查看报告 */
    var onPassPackageDoneClickAction: (item: ClawAgentMessageInfo, action: CardAction?, data: PassPackageReportPayloadBean?) -> Unit = { _, _, _ -> }
    /** 面试通关包 - 重新生成 */
    var onPassPackageRetryClickAction: (item: ClawAgentMessageInfo, action: CardAction?, data: PassPackageReportPayloadBean?) -> Unit = { _, _, _ -> }

    init {
        // 用户消息（靠右气泡）
        register(TYPE_USER_TEXT, { i, p -> SendViewHolder(ItemMsgUserRightBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // agent 普通文本（靠左气泡）
        register(TYPE_AGENT_TEXT, { i, p -> AgentViewHolder(ItemMsgAgentLeftBinding.inflate(i, p, false)) }) { h, item ->
            h.bind(item, getTypewriterDisplayedLength(item)) { length ->
                updateTypewriterDisplayedLength(item, length)
            }
        }
        // agent 思考中
        register(TYPE_AGENT_THINK, { i, p -> AgentThinkViewHolder(ItemMsgAgentThinkBinding.inflate(i, p, false)) }) { h, item ->
            h.bind(item, getTypewriterDisplayedLength(item)) { length ->
                updateTypewriterDisplayedLength(item, length)
            }
        }
        // 欢迎引导卡
        register(TYPE_WELCOME, { i, p -> AgentWelcomeViewHolder(ItemMsgAgentWelcomeBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 文本居中卡
        register(TYPE_TEXT_TIP, { i, p -> AgentTextTipViewHolder(ItemMsgAgentTextTipBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 核心补问卡
        register(TYPE_QUESTION,
            { i, p ->
                AgentQuestionViewHolder(ItemMsgAgentQuestionBinding.inflate(i, p, false)).apply {
                    onOptionClickListener = { item, option, queueId -> onQuestionOptionClickAction(item, option, queueId) }
                    onActionClickListener = { item, action, queueId -> onQuestionActionClickAction(item, action, queueId) }
                    onDefaultClickListener = { item, action, queueId -> onDefaultActionClickAction(item, action, queueId) }
                }
            }) { h, item -> h.bind(item) }
        register(TYPE_TABLE_FORM,
            { i, p ->
                AgentTableFormViewHolder(ItemMsgAgentTableFormBinding.inflate(i, p, false)).apply {
                    onActionClickListener = { item, action, queueId -> onQuestionActionClickAction(item, action, queueId) }
                    onSkipClickListener = { item, action, queueId -> onDefaultActionClickAction(item, action, queueId) }
                }
            }) { h, item -> h.bind(item) }
        // 公司背调入口卡
        register(TYPE_COMPANY_BACKGROUND_ENTRY,
            { i, p ->
                AgentCompanyBackgroundEntryViewHolder(ItemMsgAgentCompanyBackgroundEntryBinding.inflate(i, p, false)).apply {
                    onCompanyStartClickListener = { item, action, data -> onCompanyStartClickAction(item, action, data) }
                    onCompanySearchQueryListener = { keyword, callback -> onCompanySearchQueryAction(keyword, callback) }
                }
            }) { h, item -> h.bind(item) }
        // 公司背调报告卡：生成中
        register(TYPE_COMPANY_BACKGROUND_REPORT_GENERATING,
            { i, p -> AgentCompanyBackgroundReportGeneratingViewHolder(ItemMsgAgentCompanyBackgroundReportGeneratingBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        //公司背调报告卡：失败
        register(TYPE_COMPANY_BACKGROUND_REPORT_FAILED,
            { i, p ->
                AgentCompanyBackgroundReportFailedViewHolder(ItemMsgAgentCompanyBackgroundReportFailedBinding.inflate(i, p, false)).apply {
                    onCompanyRetryClickListener = { item, action, data -> onCompanyRetryClickAction(item, action, data) }
                }
            }) { h, item -> h.bind(item) }
        //公司背调报告卡：成功
        register(TYPE_COMPANY_BACKGROUND_REPORT_DONE,
            { i, p ->
                AgentCompanyBackgroundReportDoneViewHolder(ItemMsgAgentCompanyBackgroundReportDoneBinding.inflate(i, p, false)).apply {
                    onCompanyDoneClickListener = { item, action, data -> onCompanyDoneClickAction(item, action, data) }
                }
            }) { h, item -> h.bind(item) }
        // 模拟面试入口卡
        register(TYPE_MOCK_INTERVIEW_ENTRY,
            { i, p ->
                AgentMockInterviewEntryViewHolder(ItemMsgAgentMockInterviewEntryBinding.inflate(i, p, false)).apply {
                    onInterviewStartClickListener = { item, action, data -> onInterviewStartClickAction(item, action, data) }
                }
            }) { h, item -> h.bind(item) }
        // 模拟面试理解升级入口卡
        register(TYPE_MOCK_INTERVIEW_ENTRY_UPGRADE,
            { i, p ->
                AgentMockInterviewEntryUpgradeViewHolder(ItemMsgAgentMockInterviewEntryUpgradeBinding.inflate(i, p, false)).apply {
                    onInterviewStartClickListener = { item, action, data -> onInterviewStartClickAction(item, action, data) }
                    onInterviewNotNeedClickListener = { item, action, data -> onInterviewNotNeedClickAction(item, action, data) }
                }
            }) { h, item -> h.bind(item) }
        // 面试复盘报告卡：生成中
        register(TYPE_MOCK_INTERVIEW_REPORT_GENERATING,
            { i, p -> AgentMockInterviewReportGeneratingViewHolder(ItemMsgAgentMockInterviewReportGeneratingBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 面试复盘报告卡：失败
        register(TYPE_MOCK_INTERVIEW_REPORT_FAILED,
            { i, p ->
                AgentMockInterviewReportFailedViewHolder(ItemMsgAgentMockInterviewReportFailedBinding.inflate(i, p, false)).apply {
                    onInterviewRetryClickListener = { item, action, data -> onInterviewRetryClickAction(item, action, data) }
                }
            }) { h, item -> h.bind(item) }
        // 面试复盘报告卡：成功
        register(TYPE_MOCK_INTERVIEW_REPORT_DONE,
            { i, p ->
                AgentMockInterviewReportDoneViewHolder(ItemMsgAgentMockInterviewReportDoneBinding.inflate(i, p, false)).apply {
                    onInterviewDoneClickListener = { item, action, data -> onInterviewDoneClickAction(item, action, data) }
                }
            }) { h, item -> h.bind(item) }
        // 面试通关包报告卡：生成中
        register(TYPE_PASS_PACKAGE_REPORT_GENERATING,
            { i, p -> AgentPassPackageReportGeneratingViewHolder(ItemMsgAgentPassPackageReportGeneratingBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 面试通关包报告卡：失败
        register(TYPE_PASS_PACKAGE_REPORT_FAILED,
            { i, p ->
                AgentPassPackageReportFailedViewHolder(ItemMsgAgentPassPackageReportFailedBinding.inflate(i, p, false)).apply {
                    onPassPackageRetryClickListener = { item, action, data -> onPassPackageRetryClickAction(item, action, data) }
                }
            }) { h, item -> h.bind(item) }
        // 面试通关包报告卡：成功
        register(TYPE_PASS_PACKAGE_REPORT_DONE,
            { i, p ->
                AgentPassPackageReportDoneViewHolder(ItemMsgAgentPassPackageReportDoneBinding.inflate(i, p, false)).apply {
                    onPassPackageDoneClickListener = { item, action, data -> onPassPackageDoneClickAction(item, action, data) }
                }
            }) { h, item -> h.bind(item) }
        // 历史简历确认卡
        register(TYPE_PENDING_CONFIRM,
            { i, p ->
                AgentPendingConfirmViewHolder(ItemMsgAgentPendingConfirmBinding.inflate(i, p, false)).apply {
                    onConfirmClickListener = { onResumeConfirmClickAction(it, true, ) }
                    onRejectClickListener = { onResumeConfirmClickAction(it, false) }
                }
            }) { h, item -> h.bind(item) }
        // 简历上传成功
        register(TYPE_UPLOAD_SUCCESS, { i, p -> AgentUploadSuccessViewHolder(ItemMsgAgentUploadSuccessBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 简历上传指引
        register(TYPE_UPLOAD_WAIT,
            { i, p ->
                AgentResumeViewHolder(ItemMsgAgentResumeBinding.inflate(i, p, false)).apply {
                    onUploadClickListener = { itemData -> onUploadClickAction(itemData, false) }
                    onDefaultClickListener = { item, action, queueId -> onDefaultActionClickAction(item, action, queueId) }
                }
            }) { h, item ->
            h.bind(item)
        }
        // 简历上传失败
        register(TYPE_UPLOAD_FAILED,
            { i, p ->
                AgentUploadFailedViewHolder(ItemMsgAgentUploadFailedBinding.inflate(i, p, false)).apply {
                    onReUploadClickListener = { onUploadClickAction(it,true) }
                }
            }) { h, item -> h.bind(item) }
        // 正在寻找岗位
        register(TYPE_SEARCHING_JOBS, { i, p -> AgentSearchingJobsViewHolder(ItemMsgAgentSearchingJobsBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 暂未找到岗位
        register(TYPE_NO_JOBS_FOUND, { i, p -> AgentNoJobsFoundViewHolder(ItemMsgAgentNoJobsFoundBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 岗位匹配报告卡 // todo: v1.2 暂不支持
//        register(TYPE_MATCH_REPORT, { i, p -> AgentMatchReportViewHolder(ItemMsgAgentMatchReportBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 今日进度卡 // todo: v1.2 暂不支持
//        register(TYPE_TODAY_PROGRESS, { i, p -> AgentTodayProgressViewHolder(ItemMsgAgentTodayProgressBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 理解度升级卡
        register(TYPE_UNDERSTANDING_UPGRADE, { i, p -> AgentUnderstandingUpgradeViewHolder(ItemMsgAgentUnderstandingUpgradeBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }

        // messageType 即 itemType，未注册的类型会在 onCreateViewHolder 抛错，便于尽早暴露数据问题
        onItemViewType { position, list -> list[position].messageType }
    }

    /**
     * 思考中消息的旋转动画在可见时启动、滑出/回收时停止，
     * 避免 [ObjectAnimator] 持有已脱离窗口的 ImageView 引发泄漏。
     */
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is AgentViewHolder) holder.startTypewriter()
        if (holder is AgentThinkViewHolder) holder.startTypewriter()
        if (holder is AgentMockInterviewReportGeneratingViewHolder) holder.startRotate()
        if (holder is AgentCompanyBackgroundReportGeneratingViewHolder) holder.startRotate()
        if (holder is AgentPassPackageReportGeneratingViewHolder) holder.startRotate()
        if (holder is AgentMockInterviewEntryViewHolder) holder.startPersonEnterAnim()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is AgentViewHolder) holder.stopTypewriter()
        if (holder is AgentThinkViewHolder) holder.stopTypewriter()
        if (holder is AgentMockInterviewReportGeneratingViewHolder) holder.stopRotate()
        if (holder is AgentCompanyBackgroundReportGeneratingViewHolder) holder.stopRotate()
        if (holder is AgentPassPackageReportGeneratingViewHolder) holder.stopRotate()
        if (holder is AgentMockInterviewEntryViewHolder) holder.stopPersonEnterAnim()
        if (holder is AgentCompanyBackgroundEntryViewHolder) holder.clear()
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is AgentViewHolder) holder.stopTypewriter()
        if (holder is AgentThinkViewHolder) holder.stopTypewriter()
        if (holder is AgentMockInterviewReportGeneratingViewHolder) holder.stopRotate()
        if (holder is AgentCompanyBackgroundReportGeneratingViewHolder) holder.stopRotate()
        if (holder is AgentPassPackageReportGeneratingViewHolder) holder.stopRotate()
        if (holder is AgentMockInterviewEntryViewHolder) holder.stopPersonEnterAnim()
        if (holder is AgentCompanyBackgroundEntryViewHolder) holder.clear()
        super.onViewRecycled(holder)
    }

    /**
     * 提交新列表：基于 [DiffUtil] 做差量更新（按 [ClawAgentMessageInfo.id] 判断同一条消息，
     * 内容变化用 data class 全等比较），相比父类 [submitList] 的整表刷新可保留动画、减少重绘。
     */
    override fun submitList(list: List<ClawAgentMessageInfo>?) {
        var newList = list ?: emptyList()
        //测试数据
       // newList = ClawDataTest.getDataList(newList)
        prepareTypewriterDisplayedLength(newList)
        val result = DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
        items = newList
        result.dispatchUpdatesTo(this)
    }

    /**
     * 提交新列表前，计算每条打字机消息本次应该展示到哪个字符位置。
     *
     * 核心原则：
     * 1. 首次提交历史列表时直接展示完整内容，不回放历史打字动画。
     * 2. 新增在尾部的打字机消息从 0 开始播放。
     * 3. 已存在消息内容增长时，延续上一次展示进度，避免刷新时文字跳动。
     */
    private fun prepareTypewriterDisplayedLength(newList: List<ClawAgentMessageInfo>) {
        if (newList.isEmpty()) {
            typewriterDisplayedLengthMap.clear()
            hasSubmittedNonEmptyList = false
            return
        }
        val hadNonEmptyList = hasSubmittedNonEmptyList
        val oldContentLengthMap = items.associate { it.id to it.content.orEmpty().length }
        val newIds = newList.mapTo(mutableSetOf()) { it.id }
        // 清理已经从列表中移除的消息进度，避免 id 缓存越积越多。
        typewriterDisplayedLengthMap.keys.retainAll(newIds)
        newList.forEachIndexed { index, item ->
            if (!item.isTypewriterItem()) return@forEachIndexed
            val contentLength = item.content.orEmpty().length
            val oldLength = oldContentLengthMap[item.id]
            val currentDisplayedLength = typewriterDisplayedLengthMap[item.id]
            val displayedLength = when {
                // 首屏/历史消息恢复不播放动画，直接完整显示。
                !hadNonEmptyList -> contentLength
                // 新增到列表尾部的消息才从 0 开始打字；中间插入的历史消息直接完整显示。
                oldLength == null && (index >= items.size || index == newList.lastIndex) -> currentDisplayedLength ?: 0
                oldLength == null -> contentLength
                // 流式内容增长时，最多从旧内容末尾继续，不能直接跳到新增内容末尾。
                contentLength > oldLength -> currentDisplayedLength?.coerceAtMost(oldLength) ?: oldLength
                // 已经在播放中的消息保持当前进度，等待 ViewHolder 继续推进。
                currentDisplayedLength != null && currentDisplayedLength < contentLength -> currentDisplayedLength
                else -> contentLength
            }
            typewriterDisplayedLengthMap[item.id] = displayedLength.coerceIn(0, contentLength)
        }
        hasSubmittedNonEmptyList = true
    }

    /**
     * 获取当前消息应展示的打字机字符长度。
     */
    private fun getTypewriterDisplayedLength(item: ClawAgentMessageInfo): Int {
        if (!item.isTypewriterItem()) return item.content.orEmpty().length
        val contentLength = item.content.orEmpty().length
        // 没有缓存进度时默认完整显示，避免历史消息被误判为需要重新播放。
        return (typewriterDisplayedLengthMap[item.id] ?: contentLength).coerceIn(0, contentLength)
    }

    /**
     * 记录打字机播放进度，供列表刷新或 ViewHolder 复用后继续展示。
     */
    private fun updateTypewriterDisplayedLength(item: ClawAgentMessageInfo, length: Int) {
        if (!item.isTypewriterItem()) return
        val contentLength = item.content.orEmpty().length
        // ViewHolder 回传播放进度时统一夹到内容长度内，防止内容变化导致越界。
        typewriterDisplayedLengthMap[item.id] = length.coerceIn(0, contentLength)
    }

    private fun ClawAgentMessageInfo.isTypewriterItem(): Boolean =
        messageType == TYPE_AGENT_TEXT || messageType == TYPE_AGENT_THINK

    private inline fun <VH : RecyclerView.ViewHolder> register(
        itemType: Int,
        crossinline create: (inflater: LayoutInflater, parent: ViewGroup) -> VH,
        crossinline bind: (holder: VH, item: ClawAgentMessageInfo) -> Unit,
    ) {
        addItemType(itemType, object : OnMultiItemAdapterListener<ClawAgentMessageInfo, VH> {
            override fun onCreate(context: Context, parent: ViewGroup, viewType: Int): VH =
                create(LayoutInflater.from(context), parent)

            override fun onBind(holder: VH, position: Int, item: ClawAgentMessageInfo?) {
                item?.let { bind(holder, it) }
            }
        })
    }

    private class MessageDiffCallback(
        private val oldList: List<ClawAgentMessageInfo>,
        private val newList: List<ClawAgentMessageInfo>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    companion object {
        /** 用户消息 */
        const val TYPE_USER_TEXT = 0

        /** agent 普通文本 */
        const val TYPE_AGENT_TEXT = 1

        /** agent 思考中 */
        const val TYPE_AGENT_THINK = 2

        /** 欢迎引导卡（含上传简历入口） */
        const val TYPE_WELCOME = 3

        /** 核心补问卡 */
        const val TYPE_QUESTION = 4

        /** 历史简历确认卡 */
        const val TYPE_PENDING_CONFIRM = 5

        /** 简历上传成功 */
        const val TYPE_UPLOAD_SUCCESS = 6

        /** 引导简历上传 */
        const val TYPE_UPLOAD_WAIT = 7

        /** 简历上传失败 */
        const val TYPE_UPLOAD_FAILED = 8

        /** 正在寻找岗位 */
        const val TYPE_SEARCHING_JOBS = 9

        /** 暂未找到岗位 */
        const val TYPE_NO_JOBS_FOUND = 10

        /** 岗位匹配报告卡 */
        const val TYPE_MATCH_REPORT = 11

        /** 今日进度卡 */
        const val TYPE_TODAY_PROGRESS = 12

        /** 理解度升级卡 */
        const val TYPE_UNDERSTANDING_UPGRADE = 13

        /** 表单相关 - 如工作经历、教育经历、技能、项目经历、证书、奖项 */
        const val TYPE_TABLE_FORM = 14

        /** 背景调查入口卡 */
        const val TYPE_COMPANY_BACKGROUND_ENTRY = 15

        /** 公司背调报告 - 生成中 */
        const val TYPE_COMPANY_BACKGROUND_REPORT_GENERATING = 16

        /** 公司背调报告 - 生成失败 */
        const val TYPE_COMPANY_BACKGROUND_REPORT_FAILED = 17

        /** 公司背调报告 - 生成成功 */
        const val TYPE_COMPANY_BACKGROUND_REPORT_DONE = 18

        /** 模拟面试入口卡 */
        const val TYPE_MOCK_INTERVIEW_ENTRY = 19

        /** 面试复盘报告 - 生成中 */
        const val TYPE_MOCK_INTERVIEW_REPORT_GENERATING = 20

        /** 面试复盘报告 - 生成失败 */
        const val TYPE_MOCK_INTERVIEW_REPORT_FAILED = 21

        /** 面试复盘报告 - 生成成功 */
        const val TYPE_MOCK_INTERVIEW_REPORT_DONE = 22

        /** 模拟面试升级卡 */
        const val TYPE_MOCK_INTERVIEW_ENTRY_UPGRADE = 23

        /** 展示文本居中显示 */
        const val TYPE_TEXT_TIP = 24

        /** 面试通关包报告 - 生成中 */
        const val TYPE_PASS_PACKAGE_REPORT_GENERATING = 25

        /** 面试通关包报告 - 生成失败 */
        const val TYPE_PASS_PACKAGE_REPORT_FAILED = 26

        /** 面试通关包报告 - 生成成功 */
        const val TYPE_PASS_PACKAGE_REPORT_DONE = 27
    }

}
