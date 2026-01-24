package com.kgmedia.fastfilm.feature.wallet.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.response.WithdrawalRecordInfo
import com.kgmedia.fastfilm.databinding.ItemWithdrawalDetailsBinding
import com.nhsd.basic.item.BindingViewHolder

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:提现明细
 * @Data: 2025/8/6/周三  16:51
 * @Version: 1.0
 * *****************************************
 */
class WithdrawalDetailsAdapter() : PagingDataAdapter<WithdrawalRecordInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemWithdrawalDetailsBinding.inflate(
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


    inner class ViewHolder(binding: ItemWithdrawalDetailsBinding) :
        BindingViewHolder<ItemWithdrawalDetailsBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemWithdrawalDetailsVm::class.java)
                binding.item = transVM
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WithdrawalRecordInfo>() {
            override fun areItemsTheSame(
                oldItem: WithdrawalRecordInfo,
                newItem: WithdrawalRecordInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: WithdrawalRecordInfo,
                newItem: WithdrawalRecordInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}