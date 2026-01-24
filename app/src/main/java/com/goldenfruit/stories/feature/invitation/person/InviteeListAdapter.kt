package com.kgmedia.fastfilm.feature.invitation.person

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.response.InviteUserPageInfo
import com.kgmedia.fastfilm.databinding.ItemInviteeListBinding
import com.nhsd.basic.item.BindingViewHolder

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请人列表
 * @Data: 2025/8/6/周三  16:51
 * @Version: 1.0
 * *****************************************
 */
class InviteeListAdapter() : PagingDataAdapter<InviteUserPageInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemInviteeListBinding.inflate(
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


    inner class ViewHolder(binding: ItemInviteeListBinding) :
        BindingViewHolder<ItemInviteeListBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemInviteeListVm::class.java)
                binding.item = transVM
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<InviteUserPageInfo>() {
            override fun areItemsTheSame(
                oldItem: InviteUserPageInfo,
                newItem: InviteUserPageInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: InviteUserPageInfo,
                newItem: InviteUserPageInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}