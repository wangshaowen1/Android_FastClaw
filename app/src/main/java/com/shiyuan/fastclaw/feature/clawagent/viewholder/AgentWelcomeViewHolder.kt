package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.databinding.ItemMsgAgentWelcomeBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentItemEvent
import com.shiyuan.fastclaw.feature.clawagent.bean.CandidateJobAgentClientActionEnum
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.router.AppNavigator

/** agent欢迎引导卡（UPLOAD_RESUME 的 WAITING_UPLOAD / PARSING 引导态，含上传简历入口）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentWelcomeViewHolder(
    private val binding: ItemMsgAgentWelcomeBinding,
    private val itemEvents: ItemEventsViewModel<ClawAgentItemEvent>,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo, isLastItem: Boolean) {
        if (isLastItem) {
            binding.layoutAction.visible()
        } else {
            binding.layoutAction.gone()
        }
        binding.btnUploadResume.setOnSingleClickListener {
            itemEvents.postEvent(ClawAgentItemEvent.UploadResume(item, false))
        }
        binding.tvNotResume.setOnSingleClickListener {
            if (!AppNavigator.checkLoginIntercepted(false)) {
                itemEvents.postEvent(
                    ClawAgentItemEvent.DefaultAction(
                        item,
                        CardAction(CandidateJobAgentClientActionEnum.NO_RESUME_START_QUESTIONING.name),
                        null,
                    )
                )
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
