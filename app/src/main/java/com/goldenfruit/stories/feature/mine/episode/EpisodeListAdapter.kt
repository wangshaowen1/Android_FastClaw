package com.kgmedia.fastfilm.feature.mine.episode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.MyTvSeriesListInfo
import com.kgmedia.fastfilm.databinding.ItemEpisodeListBinding
import com.nhsd.basic.item.BindingViewHolder
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:小说列表
 * @Data: 2025/8/6/周三  16:51
 * @Version: 1.0
 * *****************************************
 */
class EpisodeListAdapter(
    private val eventVm: ItemEventsViewModel<EpisodeListEvent>,
    private val listingType: Int,
) : PagingDataAdapter<MyTvSeriesListInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemEpisodeListBinding.inflate(
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


    inner class ViewHolder(binding: ItemEpisodeListBinding) :
        BindingViewHolder<ItemEpisodeListBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemEpisodeListVm, MyTvSeriesListInfo>(
                    key = item.dramasSeriesId,
                    item = item,
                    viewModelFactory = {
                        ItemEpisodeListVm(eventVm, listingType,item)
                    },
                    initCallback = { }
                )
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MyTvSeriesListInfo>() {
            override fun areItemsTheSame(
                oldItem: MyTvSeriesListInfo,
                newItem: MyTvSeriesListInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: MyTvSeriesListInfo,
                newItem: MyTvSeriesListInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}