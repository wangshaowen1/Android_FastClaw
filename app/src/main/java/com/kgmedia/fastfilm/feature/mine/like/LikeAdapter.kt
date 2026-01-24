package com.kgmedia.fastfilm.feature.mine.like

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.MyFavoriteScriptInfo
import com.kgmedia.fastfilm.databinding.ItemLikeBinding
import com.nhsd.basic.item.BindingViewHolder
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:喜欢
 * @Data: 2025/8/6/周三  16:51
 * @Version: 1.0
 * *****************************************
 */
class LikeAdapter(
    private val eventVm: ItemEventsViewModel<LikeEvent>
) : PagingDataAdapter<MyFavoriteScriptInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemLikeBinding.inflate(
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


    inner class ViewHolder(binding: ItemLikeBinding) :
        BindingViewHolder<ItemLikeBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemLikeVm, MyFavoriteScriptInfo>(
                    key = null,
                    item = item,
                    viewModelFactory = {
                        ItemLikeVm(eventVm, item)
                    },
                    initCallback = { }
                )
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MyFavoriteScriptInfo>() {
            override fun areItemsTheSame(
                oldItem: MyFavoriteScriptInfo,
                newItem: MyFavoriteScriptInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: MyFavoriteScriptInfo,
                newItem: MyFavoriteScriptInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}