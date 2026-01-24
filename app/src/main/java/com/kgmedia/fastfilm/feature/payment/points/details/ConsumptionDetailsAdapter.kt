package com.kgmedia.fastfilm.feature.payment.points.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.PointConsumePageInfo
import com.kgmedia.fastfilm.databinding.ItemConsumptionDetailsBinding
import com.nhsd.basic.item.BindingViewHolder

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:积分消耗明细
 * @Data: 2025/9/10/周三  17:52
 * @Version: 1.0
 * *****************************************
 */
class ConsumptionDetailsAdapter : PagingDataAdapter<PointConsumePageInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemConsumptionDetailsBinding.inflate(
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


    inner class ViewHolder(binding: ItemConsumptionDetailsBinding) :
        BindingViewHolder<ItemConsumptionDetailsBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemRechargeDetailsVm::class.java)
                binding.item = transVM
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PointConsumePageInfo>() {
            override fun areItemsTheSame(
                oldItem: PointConsumePageInfo,
                newItem: PointConsumePageInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: PointConsumePageInfo,
                newItem: PointConsumePageInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }
        }
    }
}