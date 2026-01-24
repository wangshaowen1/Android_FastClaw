package com.kgmedia.fastfilm.feature.hit.tutorial

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.network.apis.entity.StrategyInfo
import com.kgmedia.fastfilm.databinding.ItemTutorialChildBinding
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.nhsd.basic.item.BindingViewHolder

class TutorialAdapter() : PagingDataAdapter<StrategyInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemTutorialChildBinding.inflate(
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

    var onAdapterItemClick: ((StrategyInfo?) -> Unit)? = null

    inner class ViewHolder(binding: ItemTutorialChildBinding) :
        BindingViewHolder<ItemTutorialChildBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemTutorialVm::class.java).apply {
                    onItemClick.value = {
                        onAdapterItemClick?.invoke(item)
                        SimpleBrowserActivity.launch(topActivity, item.h5Url)
                    }
                }
                binding.item = transVM
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<StrategyInfo>() {
            override fun areItemsTheSame(
                oldItem: StrategyInfo,
                newItem: StrategyInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: StrategyInfo,
                newItem: StrategyInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}