package com.kgmedia.fastfilm.feature.mine.drafts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.databinding.ItemUserShotDramaBinding
import com.nhsd.basic.item.BindingViewHolder
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:绘本作品
 * @Data: 2025/8/6/周三  16:51
 * @Version: 1.0
 * *****************************************
 */
class ShortDramaDraftsAdapter(
    private val eventVm: ItemEventsViewModel<ShortDramaDraftEvent>
) : PagingDataAdapter<ShortDramaInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemUserShotDramaBinding.inflate(
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


    inner class ViewHolder(binding: ItemUserShotDramaBinding) :
        BindingViewHolder<ItemUserShotDramaBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemShortDramaInfoWorksVm, ShortDramaInfo>(
                    key = item.dramasId,
                    item = item,
                    viewModelFactory = {
                        ItemShortDramaInfoWorksVm(eventVm, item).apply {
                        }
                    },
                    initCallback = { }
                )
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