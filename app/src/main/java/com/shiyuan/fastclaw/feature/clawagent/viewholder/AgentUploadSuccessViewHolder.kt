package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadSuccessBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent简历上传成功卡（UPLOAD_RESUME 的 PARSE_SUCCESS 态）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentUploadSuccessViewHolder(private val binding: ItemMsgAgentUploadSuccessBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java) }.getOrNull()
        }
        // 标题：statusText（如「已上传」），兜底保留布局默认「简历上传成功」
        payload?.title?.takeIf { it.isNotBlank() }?.let { binding.tvTitle.text = it }
        payload?.description?.takeIf { it.isNotBlank() }?.let { binding.tvTitle.text = it }
        payload?.alignmentScore?.let {
            binding.wpvAlignProgress.progress = it
            binding.wpvAlignProgress.text = "${it}%"
        } ?: run {
            binding.wpvAlignProgress.progress = 0
            binding.wpvAlignProgress.text = "0%"
        }
        payload?.scoreChangeText?.takeIf { it.isNotBlank() }?.let { binding.tvProgressDesc.text = it }
        payload?.nextStepText?.takeIf { it.isNotBlank() }?.let { binding.tvHelping.text = it }
    }

}