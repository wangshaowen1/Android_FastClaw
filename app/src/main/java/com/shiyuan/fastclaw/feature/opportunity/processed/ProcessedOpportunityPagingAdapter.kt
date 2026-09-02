package com.shiyuan.fastclaw.feature.opportunity.processed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.nhsd.basic.item.BindingViewHolder
import com.shiyuan.fastclaw.common.ext.getVm
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityProcessedItemData
import com.shiyuan.fastclaw.databinding.ItemProcessedOpportunityBinding
import com.shiyuan.fastclaw.feature.opportunity.ItemProcessedOpportunityVM

/**
 * 已处理机会 Paging3 适配器。
 *
 * 接收 [OpportunityProcessedItemData]，为每条数据创建可复用的 [ItemProcessedOpportunityVM]，
 * 并将点击事件交还给页面处理。
 */
class ProcessedOpportunityPagingAdapter(
    private val onItemClick: (OpportunityProcessedItemData) -> Unit
) :
    PagingDataAdapter<OpportunityProcessedItemData, ProcessedOpportunityPagingAdapter.ViewHolder>(
        DIFF_CALLBACK
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemProcessedOpportunityBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    /** 已处理机会条目的 DataBinding ViewHolder。 */
    inner class ViewHolder(
        private val binding: ItemProcessedOpportunityBinding
    ) : BindingViewHolder<ItemProcessedOpportunityBinding>(binding) {

        /** 绑定当前位置的数据，并使用岗位 ID 作为 ItemVM 缓存键。 */
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                binding.item = binding.getVm(
                    key = item.jobId,
                    item = item,
                    viewModelFactory = { ItemProcessedOpportunityVM(item) }
                )
                binding.root.setOnClickListener {
                    onItemClick(item)
                }
                binding.executePendingBindings()
            }
        }
    }

    private companion object {
        /** 岗位 ID 判断是否为同一条机会，完整数据判断内容是否变化。 */
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<OpportunityProcessedItemData>() {
            override fun areItemsTheSame(
                oldItem: OpportunityProcessedItemData,
                newItem: OpportunityProcessedItemData
            ): Boolean = oldItem.jobId == newItem.jobId

            override fun areContentsTheSame(
                oldItem: OpportunityProcessedItemData,
                newItem: OpportunityProcessedItemData
            ): Boolean = oldItem == newItem
        }
    }
}
