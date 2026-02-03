package com.kgmedia.fastfilm.feature.series

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.common.ext.putVm
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.databinding.ItemSeriesEpisodeBinding
import com.nhsd.basic.item.BindingViewHolder
import com.nhsd.basic.item.ItemEventsViewModel

class EpisodeAdapter(private val eventVm: ItemEventsViewModel<SeriesEpisodeEvent>) :
    PagingDataAdapter<EpisodeInfo, EpisodeAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSeriesEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: EpisodeAdapter.ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(binding: ItemSeriesEpisodeBinding) : BindingViewHolder<ItemSeriesEpisodeBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemSeriesEpisodeVM>(position)?.also { it.updateData(item) }
                    ?: ItemSeriesEpisodeVM(eventVm, position, item).also { binding.putVm(position, it) }
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<EpisodeInfo>() {
            override fun areItemsTheSame(
                oldItem: EpisodeInfo,
                newItem: EpisodeInfo
            ): Boolean {
                return oldItem.storyId == newItem.storyId
            }

            override fun areContentsTheSame(
                oldItem: EpisodeInfo,
                newItem: EpisodeInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}