package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.ItemMsgAgentWelcomeBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent欢迎引导卡（含上传简历入口）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentWelcomeViewHolder(private val binding: ItemMsgAgentWelcomeBinding) : RecyclerView.ViewHolder(binding.root) {

    var onUploadClickListener: (itemData: ClawAgentMessageInfo) -> Unit = {}

    fun bind(item: ClawAgentMessageInfo) {
        // TODO 绑定欢迎卡数据
        binding.btnUploadResume.setOnSingleClickListener {
            onUploadClickListener(item)
        }
    }

}