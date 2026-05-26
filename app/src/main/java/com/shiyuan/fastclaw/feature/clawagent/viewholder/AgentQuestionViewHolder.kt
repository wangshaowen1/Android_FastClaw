package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.core.network.apis.response.PayLoadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentQuestionBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent核心补问卡（问题+选项）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentQuestionViewHolder(private val binding: ItemMsgAgentQuestionBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        //TODO 绑定补问卡数据（问题文案、选项、已回复回显）
        item.metaData?.let { json ->
            val jsonData = GsonUtils.fromJson<PayLoadBean>(json, PayLoadBean::class.java)
            binding.tvQuestion.text = "${jsonData.questionText}"
        }
    }

}