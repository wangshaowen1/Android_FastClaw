package com.kgmedia.fastfilm.feature.creator.continuewriting

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.core.network.apis.entity.MyTvSeriesListInfo
import com.kgmedia.fastfilm.databinding.ItemContinueWritingBinding
import com.nhsd.basic.item.BindingViewHolder

class ContinueWritingAdapter() : PagingDataAdapter<MyTvSeriesListInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemContinueWritingBinding.inflate(
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

    inner class ViewHolder(binding: ItemContinueWritingBinding) :
        BindingViewHolder<ItemContinueWritingBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemContinueWritingVm::class.java).apply {
                    onItemClick.value = {
                        EventManager.onAppTrack(AppEvent.JG_CLICK_CONTINUE_STORY_ACTION_BUTTON, GrowingIO.KEY_ALL_GENERAL_ID to item.dramasSeriesId)
                        ContinueWritingActivity.start(topActivity, item.dramasSeriesId, item.seriesName)
                    }
                }
                binding.item = transVM
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