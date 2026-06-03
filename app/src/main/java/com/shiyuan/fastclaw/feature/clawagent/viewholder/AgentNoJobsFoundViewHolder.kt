package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentNoJobsFoundBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent暂未找到岗位消息
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentNoJobsFoundViewHolder(private val binding: ItemMsgAgentNoJobsFoundBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        // 暂无数据绑定
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java) }.getOrNull()
        }

        payload?.title?.let {
            binding.tvNoJobsFoundTitle.text = "$it"
        }

        payload?.description?.let {
            binding.tvNoJobsFoundDesc.text = "$it"
        }

    }

}