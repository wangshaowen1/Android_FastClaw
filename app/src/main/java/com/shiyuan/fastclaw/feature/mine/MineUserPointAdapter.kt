package com.shiyuan.fastclaw.feature.mine

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.item.BindingViewHolder
import com.shiyuan.fastclaw.common.ext.getVm
import com.shiyuan.fastclaw.core.network.apis.response.UserPointItemData
import com.shiyuan.fastclaw.databinding.ItemShrimpChipsBinding

/**
 * @createtime 2026年05月13日 19:59
 * @author TooWhiteT
 */

class MineUserPointAdapter() : PagingDataAdapter<UserPointItemData, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(ItemShrimpChipsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(position)
        }
    }

    inner class ViewHolder(binding: ItemShrimpChipsBinding) :
        BindingViewHolder<ItemShrimpChipsBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val itemVm = binding.getVm(item.id, item, viewModelFactory = {
                    MineUserPointItemVm(item)
                })
                binding.item = itemVm
                binding.executePendingBindings()
            }
        }

    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UserPointItemData>() {
            override fun areItemsTheSame(
                oldItem: UserPointItemData,
                newItem: UserPointItemData
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: UserPointItemData,
                newItem: UserPointItemData
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}