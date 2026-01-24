package com.kgmedia.fastfilm.feature.message.likes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.common.ext.putVm
import com.kgmedia.fastfilm.core.network.apis.entity.SystemMessageLikeInfo
import com.kgmedia.fastfilm.databinding.ItemSysMsgLikesBinding
import com.nhsd.basic.item.BindingViewHolder

class SystemMessageLikesAdapter() :
    PagingDataAdapter<SystemMessageLikeInfo, SystemMessageLikesAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSysMsgLikesBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(binding: ItemSysMsgLikesBinding) : BindingViewHolder<ItemSysMsgLikesBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm(position) ?: ItemMessageLikesVM(item).also {
                    binding.putVm(position, it)
                }
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SystemMessageLikeInfo>() {
            override fun areItemsTheSame(
                oldItem: SystemMessageLikeInfo,
                newItem: SystemMessageLikeInfo
            ): Boolean {
                return oldItem.likeTargetBusId == newItem.likeTargetBusId && oldItem.likeUserId == newItem.likeUserId && oldItem.createTime == newItem.createTime
            }

            override fun areContentsTheSame(
                oldItem: SystemMessageLikeInfo,
                newItem: SystemMessageLikeInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}