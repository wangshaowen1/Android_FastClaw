package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.ItemEventsViewModel
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadFailedBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentItemEvent
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent简历上传失败卡（UPLOAD_RESUME 的 PARSE_FAILED 态）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentUploadFailedViewHolder(
    private val binding: ItemMsgAgentUploadFailedBinding,
    private val itemEvents: ItemEventsViewModel<ClawAgentItemEvent>,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo, isLastItem: Boolean) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java) }.getOrNull()
        }
        val description = payload?.description?.replace("⚠", "⚠\uFE0F")
        binding.tvDesc.text = description?:binding.root.context.getString(R.string.upload_failed_detail)
        val enabled = payload?.interactionState?.disabled != true
        // 上传失败卡只有在列表最后一条时展示重新上传按钮，历史失败卡只保留失败说明。
        binding.btnReUpload.visibility = if (isLastItem) View.VISIBLE else View.GONE
        binding.btnReUpload.isEnabled = enabled
        binding.btnReUpload.alpha = if (enabled) 1f else 0.5f
        binding.btnReUpload.setOnSingleClickListener {
            if (enabled) itemEvents.postEvent(ClawAgentItemEvent.UploadResume(item, true))
        }
    }

}
