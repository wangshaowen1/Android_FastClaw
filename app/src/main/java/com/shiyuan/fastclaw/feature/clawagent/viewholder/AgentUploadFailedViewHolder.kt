package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUploadFailedBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent简历上传失败卡（UPLOAD_RESUME 的 PARSE_FAILED 态）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentUploadFailedViewHolder(private val binding: ItemMsgAgentUploadFailedBinding) : RecyclerView.ViewHolder(binding.root) {

    /** 重新上传点击（复用上传入口回调，重新走上传 + 提交流程） */
    var onReUploadClickListener: (item: ClawAgentMessageInfo) -> Unit = {}

    fun bind(item: ClawAgentMessageInfo) {
        binding.btnReUpload.setOnSingleClickListener { onReUploadClickListener(item) }
    }

}