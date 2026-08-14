package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.UnderstandingSummaryPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentUnderstandingUpgradeBinding
import com.shiyuan.fastclaw.databinding.ItemUnderstandingRowBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo
import com.shiyuan.fastclaw.feature.mine.OnlineResumeActivity

/** agent理解度升级卡（UNDERSTANDING_SUMMARY 理解度摘要卡）
 * @createtime 2026年05月25日 15:05
 * @author TooWhiteT
 */

class AgentUnderstandingUpgradeViewHolder(private val binding: ItemMsgAgentUnderstandingUpgradeBinding) : RecyclerView.ViewHolder(binding.root) {

    /** 上次动态添加的明细行，复用前先移除，避免累积 */
    private val detailRows = ArrayList<View>()

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, UnderstandingSummaryPayloadBean::class.java) }.getOrNull()
        }
        val context = binding.root.context
        // 对齐度进度（0-100），同步进度环文案
        val score = (payload?.understandingScore ?: 0).coerceIn(0, 100)
        binding.wpvAlignProgress.progress = score
        binding.wpvAlignProgress.text = "$score%"
        binding.tvAlignTitle.text = context.getString(R.string.str_agent_understanding_upgrade_title)
        //binding.tvAlignSubtitle.text = payload?.description?.takeIf { it.isNotBlank() } ?: context.getString(R.string.str_agent_understanding_align_subtitle, score)
        binding.tvAlignSubtitle.text = context.getString(R.string.str_agent_understanding_align_subtitle)
        binding.clBubbleLayout.setOnSingleClickListener {
            OnlineResumeActivity.start(context,1)
        }
        binding.clUnderstandingDetail.setOnSingleClickListener {
            OnlineResumeActivity.start(context,0)
        }
        // 理解明细行
        bindDetailRows(payload?.summary?.fields)
    }

    /** 按 summary.fields 重建明细行：先移除旧行，再 inflate + addView，最后交给 Flow 垂直排列 */
    private fun bindDetailRows(fields: List<UnderstandingSummaryPayloadBean.SummaryField>?) {
        val parent = binding.clUnderstandingDetail
        detailRows.forEach { parent.removeView(it) }
        detailRows.clear()
        if (fields.isNullOrEmpty()) {
            binding.flowDetailRows.referencedIds = IntArray(0)
            return
        }
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val ids = IntArray(fields.size)
        fields.forEachIndexed { index, field ->
            val rowBinding = ItemUnderstandingRowBinding.inflate(inflater, parent, false)
            rowBinding.tvRowLabel.text = field.fieldName.orEmpty()
            val value = field.displayValue
            if (value.isNullOrEmpty()) {
                // 未补齐：灰字占位
                rowBinding.tvRowValue.text = context.getString(R.string.str_agent_understanding_field_unaligned)
                rowBinding.tvRowValue.setTextColor(ContextCompat.getColor(context, R.color.color_56_3c3c43))
            } else {
                rowBinding.tvRowValue.text = value
                rowBinding.tvRowValue.setTextColor(ContextCompat.getColor(context, R.color.color_90_000000))
            }
            val row = rowBinding.root.apply { id = View.generateViewId() }
            parent.addView(row)
            ids[index] = row.id
            detailRows.add(row)
        }
        binding.flowDetailRows.referencedIds = ids
    }

}
