package com.kgmedia.fastfilm.feature.message.watching

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.common.ext.putVm
import com.kgmedia.fastfilm.core.network.apis.entity.SystemMessageWatchingInfo
import com.kgmedia.fastfilm.databinding.ItemSysMsgWatchingBinding
import com.nhsd.basic.item.BindingViewHolder

class SystemMessageWatchingAdapter() :
    PagingDataAdapter<SystemMessageWatchingInfo, SystemMessageWatchingAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSysMsgWatchingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(binding: ItemSysMsgWatchingBinding) : BindingViewHolder<ItemSysMsgWatchingBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm(position) ?: ItemMessageWatchingVM(item).also {
                    binding.putVm(position, it)
                }
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SystemMessageWatchingInfo>() {
            override fun areItemsTheSame(
                oldItem: SystemMessageWatchingInfo,
                newItem: SystemMessageWatchingInfo
            ): Boolean {
                return oldItem.dramasSeriesId == newItem.dramasSeriesId && oldItem.watchingUserId == newItem.watchingUserId && oldItem.createTime == newItem.createTime
            }

            override fun areContentsTheSame(
                oldItem: SystemMessageWatchingInfo,
                newItem: SystemMessageWatchingInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}