package com.kgmedia.fastfilm.feature.invitation.income

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.response.InviteIncomePageInfo
import com.kgmedia.fastfilm.databinding.ItemInvitationIncomeBinding
import com.nhsd.basic.item.BindingViewHolder

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请收益列表
 * @Data: 2025/8/6/周三  16:51
 * @Version: 1.0
 * *****************************************
 */
class InvitationIncomeAdapter() : PagingDataAdapter<InviteIncomePageInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemInvitationIncomeBinding.inflate(
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


    inner class ViewHolder(binding: ItemInvitationIncomeBinding) :
        BindingViewHolder<ItemInvitationIncomeBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemInvitationIncomeVm::class.java)
                binding.item = transVM
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<InviteIncomePageInfo>() {
            override fun areItemsTheSame(
                oldItem: InviteIncomePageInfo,
                newItem: InviteIncomePageInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: InviteIncomePageInfo,
                newItem: InviteIncomePageInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}