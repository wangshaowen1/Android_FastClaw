package com.kgmedia.fastfilm.feature.mine.chase

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.TheTvSeriesIFollowInfo
import com.kgmedia.fastfilm.databinding.ItemBingeWatchBinding
import com.nhsd.basic.item.BindingViewHolder
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:追剧
 * @Data: 2025/8/6/周三  16:51
 * @Version: 1.0
 * *****************************************
 */
class BingeWatchAdapter(
    private val eventVm: ItemEventsViewModel<BingeWatchEvent>
) : PagingDataAdapter<TheTvSeriesIFollowInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemBingeWatchBinding.inflate(
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


    inner class ViewHolder(binding: ItemBingeWatchBinding) :
        BindingViewHolder<ItemBingeWatchBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemBingeWatchVm, TheTvSeriesIFollowInfo>(
                    key = null,
                    item = item,
                    viewModelFactory = {
                        ItemBingeWatchVm(eventVm, item)
                    },
                    initCallback = { }
                )
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TheTvSeriesIFollowInfo>() {
            override fun areItemsTheSame(
                oldItem: TheTvSeriesIFollowInfo,
                newItem: TheTvSeriesIFollowInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: TheTvSeriesIFollowInfo,
                newItem: TheTvSeriesIFollowInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}