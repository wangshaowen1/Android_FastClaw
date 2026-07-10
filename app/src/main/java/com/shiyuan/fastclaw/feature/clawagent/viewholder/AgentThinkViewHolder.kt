package com.shiyuan.fastclaw.feature.clawagent.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.kuaige.imageloader.GlideApp
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ItemMsgAgentThinkBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/** agent思考消息
 * @createtime 2026年05月25日 14:51
 * @author TooWhiteT
 */

class AgentThinkViewHolder(private val binding: ItemMsgAgentThinkBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClawAgentMessageInfo) {
        binding.tvThink.text = "${item.content}"
    }

    /** 播放思考中的 gif；附着到窗口时由 Adapter 调用 */
    fun startRotate() {
        GlideApp.with(binding.ivThink)
            .asGif()
            .load(R.drawable.icon_agent_think)
            .into(binding.ivThink)
    }

    /** 停止 gif；脱离窗口或回收时由 Adapter 调用 */
    fun stopRotate() {
        GlideApp.with(binding.ivThink).clear(binding.ivThink)
    }

}
