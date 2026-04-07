package com.kgmedia.fastfilm.feature.works.assetmanager.roleimage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.RoleImageInfo
import com.kgmedia.fastfilm.databinding.ItemAssetRoleImageBinding
import com.nhsd.basic.item.BaseItemEventsViewModel
import com.nhsd.basic.item.BindingViewHolder

class RoleImageAdapter(
    val model: Boolean = false,
    val itemClickVm: BaseItemEventsViewModel<RoleImageEvent, RoleImageInfo>,
    val appearanceId: String? = null
) :
    PagingDataAdapter<RoleImageInfo, RecyclerView.ViewHolder>(ITEM_DIFF_CALLBACK) {

    companion object {

        private val ITEM_DIFF_CALLBACK = object : DiffUtil.ItemCallback<RoleImageInfo>() {
            override fun areItemsTheSame(oldItem: RoleImageInfo, newItem: RoleImageInfo): Boolean {
                return oldItem.appearanceId == newItem.appearanceId
            }

            override fun areContentsTheSame(oldItem: RoleImageInfo, newItem: RoleImageInfo): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemAssetRoleImageBinding.inflate(
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

    inner class ViewHolder(binding: ItemAssetRoleImageBinding) : BindingViewHolder<ItemAssetRoleImageBinding>(binding) {
        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                // 使用 getVm 维护状态
                val vm = binding.getVm<ItemRoleImageVm, RoleImageInfo>(key = item.appearanceId, item, viewModelFactory = {
                    ItemRoleImageVm(item, model, itemClickVm).apply {
                        if (appearanceId == item.appearanceId) {
                            selectModel.set(true)
                        }
                    }
                },{})
                vm.updateData(item)
                binding.item = vm
                binding.executePendingBindings()
            }
        }
    }

}