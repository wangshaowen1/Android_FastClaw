package com.kgmedia.fastfilm.feature.editor.dubbing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.DramasTimbreInfo
import com.kgmedia.fastfilm.databinding.ItemDubbingTimbreBinding
import com.nhsd.basic.item.BindingViewHolder
import com.nhsd.basic.item.ItemEventsViewModel

class DubbingTimbreAdapter(private val eventVm: ItemEventsViewModel<DubbingTimbreEvent>) : PagingDataAdapter<DramasTimbreInfo, DubbingTimbreAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemDubbingTimbreBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: DubbingTimbreAdapter.ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(binding: ItemDubbingTimbreBinding) : BindingViewHolder<ItemDubbingTimbreBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val viewModel = binding.getVm<ItemDubbingTimbreVM, DramasTimbreInfo>(
                    key = item.timbreId,
                    item = item,
                    viewModelFactory = {
                        ItemDubbingTimbreVM(eventVm, item).also {
                            if (item.selectStatus == 1) {
                                eventVm.postEvent(DubbingTimbreEvent.Click(it))
                            }
                        }
                    }
                )
                binding.item = viewModel
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DramasTimbreInfo>() {
            override fun areItemsTheSame(
                oldItem: DramasTimbreInfo,
                newItem: DramasTimbreInfo
            ): Boolean {
                return oldItem.timbreId == newItem.timbreId
            }

            override fun areContentsTheSame(
                oldItem: DramasTimbreInfo,
                newItem: DramasTimbreInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}