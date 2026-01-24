package com.kgmedia.fastfilm.feature.message.systemnotice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.common.ext.putVm
import com.kgmedia.fastfilm.core.network.apis.entity.SystemNoticeInfo
import com.kgmedia.fastfilm.databinding.ItemSystemNoticeBinding
import com.nhsd.basic.item.BindingViewHolder
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * 系统通知
 */
class SystemNoticeAdapter(private val eventVM: ItemEventsViewModel<ItemSystemNoticeEvent>) : PagingDataAdapter<SystemNoticeInfo, SystemNoticeAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSystemNoticeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SystemNoticeAdapter.ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(binding: ItemSystemNoticeBinding) : BindingViewHolder<ItemSystemNoticeBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm(position) ?: ItemSystemNoticeVM(eventVM, item).also {
                    binding.putVm(position, it)
                }
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SystemNoticeInfo>() {
            override fun areItemsTheSame(
                oldItem: SystemNoticeInfo,
                newItem: SystemNoticeInfo
            ): Boolean {
                return oldItem.messagesId == newItem.messagesId
            }

            override fun areContentsTheSame(
                oldItem: SystemNoticeInfo,
                newItem: SystemNoticeInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}