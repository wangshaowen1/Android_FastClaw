package com.shiyuan.fastclaw.feature.clawagent.message

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentSearchingJobsBinding
import kotlin.jvm.java

/** agent正在寻找岗位消息
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentSearchingJobsViewHolder(private val binding: ItemMsgAgentSearchingJobsBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        // 暂无数据绑定
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java) }.getOrNull()
        }

        payload?.title?.let {
            binding.tvSearchingJobsTitle.text = "$it"
        }
        
        payload?.description?.let {
            binding.tvSearchingJobsDesc.text = "$it"
        }
    }

}