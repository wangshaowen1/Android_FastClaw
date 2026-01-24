package com.kgmedia.fastfilm.feature.payment.points.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.response.RechargePageInfo
import com.kgmedia.fastfilm.databinding.ItemRechargeDetailsBinding
import com.nhsd.basic.item.BindingViewHolder

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:充值明细
 * @Data: 2025/9/10/周三  17:52
 * @Version: 1.0
 * *****************************************
 */
class RechargeDetailsAdapter  : PagingDataAdapter<RechargePageInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemRechargeDetailsBinding.inflate(
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


    inner class ViewHolder(binding: ItemRechargeDetailsBinding) :
        BindingViewHolder<ItemRechargeDetailsBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemConsumptionDetailsVm::class.java)
                binding.item = transVM
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RechargePageInfo>() {
            override fun areItemsTheSame(
                oldItem: RechargePageInfo,
                newItem: RechargePageInfo
            ): Boolean {
                return oldItem.userScoreDetailsId == newItem.userScoreDetailsId
            }

            override fun areContentsTheSame(
                oldItem: RechargePageInfo,
                newItem: RechargePageInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }
        }
    }
}