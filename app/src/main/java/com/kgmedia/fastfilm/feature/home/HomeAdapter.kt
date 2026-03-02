package com.kgmedia.fastfilm.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.databinding.ItemHomeDramaBinding
import com.kgmedia.fastfilm.feature.play.DramasPlayPagerActivity
import com.nhsd.basic.item.BindingViewHolder

class HomeAdapter() : PagingDataAdapter<WorkInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemHomeDramaBinding.inflate(
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


    inner class ViewHolder(binding: ItemHomeDramaBinding) :
        BindingViewHolder<ItemHomeDramaBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemHomeDramaVm::class.java).apply {
                    onItemClick.value = {
                        EventManager.onAppTrack(AppEvent.SF_CLICK_HOME_RECOMMEND_VIDEO)
                        DramasPlayPagerActivity.start(null, position)
                    }
                }
                binding.item = transVM
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WorkInfo>() {
            override fun areItemsTheSame(
                oldItem: WorkInfo,
                newItem: WorkInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: WorkInfo,
                newItem: WorkInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}