package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.UploadResumePayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentWelcomeBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent欢迎引导卡（UPLOAD_RESUME 的 WAITING_UPLOAD / PARSING 引导态，含上传简历入口）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentWelcomeViewHolder(private val binding: ItemMsgAgentWelcomeBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {

    }

}