package com.shiyuan.fastclaw.feature.mine

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.item.BindingViewHolder
import com.shiyuan.fastclaw.common.ext.getVm
import com.shiyuan.fastclaw.databinding.ItemInterviewReportDoneBinding
import com.shiyuan.fastclaw.databinding.ItemInterviewReportFailedBinding
import com.shiyuan.fastclaw.databinding.ItemInterviewReportInProgressBinding
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus

class InterviewReportAdapter(
    private val onItemClick: (ReportInfo) -> Unit
) : PagingDataAdapter<ReportInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)?.status) {
            ReportStatus.IN_PROGRESS -> VIEW_TYPE_IN_PROGRESS
            ReportStatus.FAILED -> VIEW_TYPE_FAILED
            else -> VIEW_TYPE_DONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_IN_PROGRESS -> InProgressViewHolder(
                ItemInterviewReportInProgressBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_FAILED -> FailedViewHolder(
                ItemInterviewReportFailedBinding.inflate(inflater, parent, false)
            )
            else -> DoneViewHolder(
                ItemInterviewReportDoneBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is InProgressViewHolder -> holder.bind(position)
            is FailedViewHolder -> holder.bind(position)
            is DoneViewHolder -> holder.bind(position)
        }
    }

    private fun buildItemVm(item: ReportInfo) = ItemInterviewReportVM(item)

    inner class InProgressViewHolder(
        private val binding: ItemInterviewReportInProgressBinding
    ) : BindingViewHolder<ItemInterviewReportInProgressBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                binding.item = binding.getVm(
                    key = item.id,
                    item = item,
                    viewModelFactory = { buildItemVm(item) }
                ).apply {
                    onItemClick.value = { onItemClick(item) }
                }
                binding.executePendingBindings()
            }
        }
    }

    inner class FailedViewHolder(
        private val binding: ItemInterviewReportFailedBinding
    ) : BindingViewHolder<ItemInterviewReportFailedBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                binding.item = binding.getVm(
                    key = item.id,
                    item = item,
                    viewModelFactory = { buildItemVm(item) }
                ).apply {
                    onItemClick.value = { onItemClick(item) }
                }
                binding.executePendingBindings()
            }
        }
    }

    inner class DoneViewHolder(
        private val binding: ItemInterviewReportDoneBinding
    ) : BindingViewHolder<ItemInterviewReportDoneBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                binding.item = binding.getVm(
                    key = item.id,
                    item = item,
                    viewModelFactory = { buildItemVm(item) }
                ).apply {
                    onItemClick.value = { onItemClick(item) }
                }
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_IN_PROGRESS = 1
        private const val VIEW_TYPE_FAILED = 2
        private const val VIEW_TYPE_DONE = 3

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
