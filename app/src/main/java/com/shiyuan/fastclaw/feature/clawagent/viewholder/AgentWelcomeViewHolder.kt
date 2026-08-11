package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.databinding.ItemMsgAgentWelcomeBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.router.AppNavigator

/** agent欢迎引导卡（UPLOAD_RESUME 的 WAITING_UPLOAD / PARSING 引导态，含上传简历入口）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentWelcomeViewHolder(private val binding: ItemMsgAgentWelcomeBinding) : RecyclerView.ViewHolder(binding.root) {

    var onUploadClickListener: (item: ClawAgentMessageInfo) -> Unit = {}

    /** 我没有简历 */
    var onDefaultClickListener: (item: ClawAgentMessageInfo, action: CardAction, questionQueueId: String?) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo, isLastItem: Boolean) {
        if (isLastItem) {
            binding.tvBriefDesc.gone()
            binding.tvOpportunityTitle.visible()
            binding.tvOpportunityDesc.visible()
            binding.layoutAction.visible()
        } else {
            binding.tvBriefDesc.visible()
            binding.tvOpportunityTitle.gone()
            binding.tvOpportunityDesc.gone()
            binding.layoutAction.gone()
        }
        val layoutParams = binding.tvAiTag.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.verticalBias = if (isLastItem) 0f else 0.5f
        layoutParams.topMargin = if (isLastItem) 8.dp else 0.dp
        binding.tvAiTag.layoutParams = layoutParams

        binding.btnUploadResume.setOnSingleClickListener {
            onUploadClickListener(item)
        }
        binding.tvNotResume.setOnSingleClickListener {
            if (!AppNavigator.checkLoginIntercepted(false)) {
                onDefaultClickListener(item, CardAction(CandidateJobAgentClientActionEnum.NO_RESUME_START_QUESTIONING.name), null)
            }
        }
        //updateContainerMargin()
    }

    private fun updateContainerMargin() {
        itemView.post {
            val recyclerView = itemView.parent as? RecyclerView ?: return@post
            val itemHeight = itemView.height.takeIf { it > 0 } ?: return@post
            val bottomMargin = WELCOME_BOTTOM_SPACE.dp
            val parentHeight = (recyclerView.parent as? View)?.height?.takeIf { it > 0 } ?: recyclerView.height
            val topMargin = (parentHeight - itemHeight - bottomMargin).coerceAtLeast(0)
            binding.layoutContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                this.topMargin = topMargin
            }
        }
    }

    companion object {
        /** 欢迎卡片底部距离列表底部的固定间距。 */
        private const val WELCOME_BOTTOM_SPACE = 60
    }
}
