package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.databinding.ItemMsgAgentWelcomeBinding
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
            binding.layoutWelcomeActions.visible()
        } else {
            binding.layoutWelcomeActions.gone()
        }
        binding.btnUploadResume.setOnSingleClickListener {
            onUploadClickListener(item)
        }
        binding.tvNotResume.setOnSingleClickListener {
            showToast(R.string.toast_resume_not_conversation)
            if (!AppNavigator.checkLoginIntercepted(false)) {
                //action?.let { onDefaultClickListener(item, action, null) }
            }
        }
    }
}
