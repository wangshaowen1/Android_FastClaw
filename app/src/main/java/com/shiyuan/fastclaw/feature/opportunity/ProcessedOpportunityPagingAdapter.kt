package com.shiyuan.fastclaw.feature.opportunity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.nhsd.basic.item.BindingViewHolder
import com.shiyuan.fastclaw.common.ext.getVm
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityProcessedItemData
import com.shiyuan.fastclaw.databinding.ItemProcessedOpportunityBinding

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

    inner class ViewHolder(
        private val binding: ItemProcessedOpportunityBinding
    ) : BindingViewHolder<ItemProcessedOpportunityBinding>(binding) {

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
