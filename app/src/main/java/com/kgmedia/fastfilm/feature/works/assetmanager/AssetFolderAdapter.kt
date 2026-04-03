package com.kgmedia.fastfilm.feature.works.assetmanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.databinding.ItemAssetFolderBinding
import com.nhsd.basic.item.BindingViewHolder

class AssetFolderAdapter : PagingDataAdapter<WorkInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemAssetFolderBinding.inflate(
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


    inner class ViewHolder(binding: ItemAssetFolderBinding) :
        BindingViewHolder<ItemAssetFolderBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm<ItemAssetFolderVm, WorkInfo>(key = item.workId, item) {
                    ItemAssetFolderVm(item)
                }
                transVM.updateData(item)
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
                return oldItem.workId == newItem.workId
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