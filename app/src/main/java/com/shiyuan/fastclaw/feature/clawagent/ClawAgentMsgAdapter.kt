package com.shiyuan.fastclaw.feature.clawagent

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseMultiItemAdapter
import com.chad.library.adapter.base.BaseMultiItemAdapter.OnMultiItemAdapterListener
import com.shiyuan.fastclaw.databinding.ItemMsgAgentLeftBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentMatchReportBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentNoJobsFoundBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentPendingConfirmBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentQuestionBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentSearchingJobsBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentThinkBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentTodayProgressBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUnderstandingUpgradeBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadFailedBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadSuccessBinding
import com.shiyuan.fastclaw.databinding.ItemMsgAgentWelcomeBinding
import com.shiyuan.fastclaw.databinding.ItemMsgUserRightBinding
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CardOption
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentMatchReportViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentNoJobsFoundViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentPendingConfirmViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentQuestionViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentSearchingJobsViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentThinkViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentTodayProgressViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentUnderstandingUpgradeViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentUploadFailedViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentUploadSuccessViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.AgentWelcomeViewHolder
import com.shiyuan.fastclaw.feature.clawagent.viewholder.SendViewHolder

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

    var onUploadClickAction: (itemData: ClawAgentMessageInfo) -> Unit = {}

    /** 补问卡选项点击（B 方案：仅暴露，提交后续统一接 sendAction(QUESTION_OPTION_SUBMIT)） */
    var onQuestionOptionClickAction: (item: ClawAgentMessageInfo, option: CardOption) -> Unit = { _, _ -> }

    /** 补问卡动作按钮（如跳过）点击 */
    var onQuestionActionClickAction: (item: ClawAgentMessageInfo, action: CardAction) -> Unit = { _, _ -> }

    /** 历史简历确认卡 确认(true)/否认(false) 点击（B 方案：仅暴露，提交后续统一接） */
    var onResumeConfirmClickAction: (item: ClawAgentMessageInfo, confirmed: Boolean) -> Unit = { _, _ -> }

    init {
        // 用户消息（靠右气泡）
        register(TYPE_USER_TEXT, { i, p -> SendViewHolder(ItemMsgUserRightBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // agent 普通文本（靠左气泡）
        register(TYPE_AGENT_TEXT, { i, p -> AgentViewHolder(ItemMsgAgentLeftBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // agent 思考中
        register(TYPE_AGENT_THINK, { i, p -> AgentThinkViewHolder(ItemMsgAgentThinkBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 欢迎引导卡
        register(TYPE_WELCOME,
            { i, p ->
                AgentWelcomeViewHolder(ItemMsgAgentWelcomeBinding.inflate(i, p, false)).apply {
                    onUploadClickListener = { itemData ->
                        onUploadClickAction(itemData)
                    }
                }
            }) { h, item ->
                h.bind(item)
            }
        // 核心补问卡
        register(TYPE_QUESTION,
            { i, p ->
                AgentQuestionViewHolder(ItemMsgAgentQuestionBinding.inflate(i, p, false)).apply {
                    onOptionClickListener = { item, option -> onQuestionOptionClickAction(item, option) }
                    onActionClickListener = { item, action -> onQuestionActionClickAction(item, action) }
                }
            }) { h, item -> h.bind(item) }
        // 历史简历确认卡
        register(TYPE_PENDING_CONFIRM,
            { i, p ->
                AgentPendingConfirmViewHolder(ItemMsgAgentPendingConfirmBinding.inflate(i, p, false)).apply {
                    onConfirmClickListener = { onResumeConfirmClickAction(it, true) }
                    onRejectClickListener = { onResumeConfirmClickAction(it, false) }
                }
            }) { h, item -> h.bind(item) }
        // 简历上传成功
        register(TYPE_UPLOAD_SUCCESS, { i, p -> AgentUploadSuccessViewHolder(ItemMsgAgentUploadSuccessBinding.inflate(i, p, false)) }) { h, item -> h.bind(item) }
        // 简历上传失败
        register(TYPE_UPLOAD_FAILED,
            { i, p ->
                AgentUploadFailedViewHolder(ItemMsgAgentUploadFailedBinding.inflate(i, p, false)).apply {
                    onReUploadClickListener = { onUploadClickAction(it) }
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
     * 提交新列表：基于 [DiffUtil] 做差量更新（按 [ClawAgentMessageInfo.id] 判断同一条消息，
     * 内容变化用 data class 全等比较），相比父类 [submitList] 的整表刷新可保留动画、减少重绘。
     */
    override fun submitList(list: List<ClawAgentMessageInfo>?) {
        val newList = list ?: emptyList()
        val result = DiffUtil.calculateDiff(MessageDiffCallback(items, newList))
        items = newList
        result.dispatchUpdatesTo(this)
    }

    /**
     * 收敛 [addItemType] 的样板：传入创建与绑定逻辑即可注册一种消息样式。
     */
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

        /** 简历上传失败 */
        const val TYPE_UPLOAD_FAILED = 7

        /** 正在寻找岗位 */
        const val TYPE_SEARCHING_JOBS = 8

        /** 暂未找到岗位 */
        const val TYPE_NO_JOBS_FOUND = 9

        /** 岗位匹配报告卡 */
        const val TYPE_MATCH_REPORT = 10

        /** 今日进度卡 */
        const val TYPE_TODAY_PROGRESS = 11

        /** 理解度升级卡 */
        const val TYPE_UNDERSTANDING_UPGRADE = 12
    }
}