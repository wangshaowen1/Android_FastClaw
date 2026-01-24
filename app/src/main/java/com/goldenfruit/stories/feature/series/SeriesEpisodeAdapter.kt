package com.kgmedia.fastfilm.feature.series

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.common.ext.putVm
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.databinding.ItemSeriesEpisodeBinding
import com.nhsd.basic.item.BindingViewHolder
import com.nhsd.basic.item.ItemEventsViewModel

class SeriesEpisodeAdapter(private val eventVm: ItemEventsViewModel<SeriesEpisodeEvent>) : PagingDataAdapter<ShortDramaInfo, SeriesEpisodeAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemSeriesEpisodeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SeriesEpisodeAdapter.ViewHolder, position: Int) {
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
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShortDramaInfo>() {
            override fun areItemsTheSame(
                oldItem: ShortDramaInfo,
                newItem: ShortDramaInfo
            ): Boolean {
                return oldItem.dramasId == newItem.dramasId
            }

            override fun areContentsTheSame(
                oldItem: ShortDramaInfo,
                newItem: ShortDramaInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}