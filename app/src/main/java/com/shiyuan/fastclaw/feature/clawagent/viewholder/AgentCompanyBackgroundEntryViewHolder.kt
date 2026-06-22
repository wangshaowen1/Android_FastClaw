package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.afterTextChanged
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundEntryPayloadBean
import com.shiyuan.fastclaw.core.network.apis.response.CompanyStartBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundEntryBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 公司背调入口卡
 */
class AgentCompanyBackgroundEntryViewHolder(private val binding: ItemMsgAgentCompanyBackgroundEntryBinding, ) : RecyclerView.ViewHolder(binding.root) {

    var onCompanyStartClickListener: (item: ClawAgentMessageInfo, action: CardAction, data: CompanyStartBean) -> Unit = { _, _, _ -> }

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CompanyBackgroundEntryPayloadBean::class.java) }.getOrNull()
        }
        val action = payload?.actions?.firstOrNull()
        //val enabled = action != null && action.disabled != true && payload?.interactionState?.disabled != true
        /*binding.tvAction.isEnabled = enabled
        binding.tvAction.alpha = if (enabled) 1f else 0.5f*/

        binding.tvAction.setOnSingleClickListener {
            action?.let {
                onCompanyStartClickListener(item, action, CompanyStartBean(binding.etSearch.text.toString()))
            }
        }

        binding.ivDelete.setOnSingleClickListener {
            binding.etSearch.setText("")
        }

        binding.etSearch.afterTextChanged {
            binding.ivDelete.visibility = if (it.toString().isEmpty()) GONE else VISIBLE
        }
    }
}
