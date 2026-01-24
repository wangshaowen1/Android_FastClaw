package com.kgmedia.fastfilm.feature.creator.character

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.kgmedia.fastfilm.databinding.ItemCharacterBinding
import com.nhsd.basic.item.BaseItemEventsViewModel
import com.nhsd.basic.item.BindingViewHolder

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:制作绘本-主角形象
 * @Data: 2025/9/9/周二  17:24
 * @Version: 1.0
 * *****************************************
 */
class CharactersAdapter(
    private val eventVm: BaseItemEventsViewModel<CharacterEvent, String>,
) : PagingDataAdapter<CharacterInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCharacterBinding.inflate(
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


    inner class ViewHolder(binding: ItemCharacterBinding) :
        BindingViewHolder<ItemCharacterBinding>(binding) {


        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                binding.apply {
                    val viewModel = getVm<ItemCharacterVm, CharacterInfo>(
                        key = item.roleId,
                        item = item,
                        viewModelFactory = {
                            ItemCharacterVm(eventVm, item)
                        },
                        initCallback = { }
                    )
                    binding.item = viewModel
                    binding.executePendingBindings()
                }
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CharacterInfo>() {
            override fun areItemsTheSame(
                oldItem: CharacterInfo,
                newItem: CharacterInfo
            ): Boolean {
                return oldItem.roleId == newItem.roleId
            }

            override fun areContentsTheSame(
                oldItem: CharacterInfo,
                newItem: CharacterInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }
        }
    }
}