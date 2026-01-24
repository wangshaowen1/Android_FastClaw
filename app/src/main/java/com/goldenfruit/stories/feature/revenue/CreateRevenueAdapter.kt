package com.kgmedia.fastfilm.feature.revenue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.ProfitDetailInfo
import com.kgmedia.fastfilm.databinding.ItemIncomeDetailsBinding
import com.nhsd.basic.item.BindingViewHolder

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:创造收益
 * @Data: 2025/11/4/周二  11:02
 * @Version: 1.0
 * *****************************************
 */
class CreateRevenueAdapter() : PagingDataAdapter<ProfitDetailInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemIncomeDetailsBinding.inflate(
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


    inner class ViewHolder(binding: ItemIncomeDetailsBinding) :
        BindingViewHolder<ItemIncomeDetailsBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemIncomeDetailsVm::class.java)
                binding.item = transVM
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ProfitDetailInfo>() {
            override fun areItemsTheSame(
                oldItem: ProfitDetailInfo,
                newItem: ProfitDetailInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: ProfitDetailInfo,
                newItem: ProfitDetailInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}