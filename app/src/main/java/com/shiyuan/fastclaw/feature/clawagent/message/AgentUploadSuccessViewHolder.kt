package com.shiyuan.fastclaw.feature.clawagent.message

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.AppEventParam.JOB_PREFERENCE
import com.shiyuan.fastclaw.core.manager.AppEventParam.KLX_VIEW_SHRIMP_UNDERSTANDS
import com.shiyuan.fastclaw.core.manager.AppEventParam.UPGRADE_EDIT
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadSuccessBinding
import com.shiyuan.fastclaw.feature.resume.OnlineResumeActivity

/** agent简历上传成功卡（UPLOAD_RESUME 的 PARSE_SUCCESS 态）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentUploadSuccessViewHolder(private val binding: ItemMsgAgentUploadSuccessBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UploadResumePayloadBean::class.java) }.getOrNull()
        }
        val context = binding.root.context
        binding.tvTitle.text = payload?.title?.takeIf { it.isNotBlank() }
            ?: binding.root.context.getString(R.string.upload_success_title)
        val alignmentScore = payload?.alignmentScore?:0
        binding.wpvAlignProgress.progress = alignmentScore
        binding.wpvAlignProgress.text = "${alignmentScore}%"
        binding.tvProgressDesc.text = payload?.scoreChangeText.orEmpty()

        binding.root.setOnSingleClickListener {
            EventManager.onAppTrack(AppEvent.KLX_VIEW_SHRIMP_UNDERSTANDS, mutableMapOf(KLX_VIEW_SHRIMP_UNDERSTANDS to UPGRADE_EDIT))
            OnlineResumeActivity.start(context,0)
        }
    }
}
