package com.shiyuan.fastclaw.feature.report

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.item.BindingViewHolder
import com.shiyuan.fastclaw.common.ext.getVm
import com.shiyuan.fastclaw.databinding.ItemReportBinding
import com.shiyuan.fastclaw.feature.report.data.ReportInfo

class ReportAdapter(
    private val onItemClick: (ReportInfo) -> Unit
) : PagingDataAdapter<ReportInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            ItemReportBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(position)
        }
    }

    inner class ViewHolder(private val binding: ItemReportBinding) : BindingViewHolder<ItemReportBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val itemVm = binding.getVm(
                    key = item.id,
                    item = item,
                    viewModelFactory = { ItemReportVM(item) }
                ).apply {
                    onItemClick.value = { onItemClick(item) }
                }
                binding.item = itemVm
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ReportInfo>() {
            override fun areItemsTheSame(oldItem: ReportInfo, newItem: ReportInfo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ReportInfo, newItem: ReportInfo): Boolean {
                return oldItem == newItem
            }
        }
    }
}
