package com.kgmedia.fastfilm.feature.creator.style

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.network.apis.entity.CreationStyleInfo
import com.kgmedia.fastfilm.databinding.ItemCreationStyleAllBinding
import com.kgmedia.fastfilm.feature.payment.vip.MembershipBenefitsActivity
import com.nhsd.basic.item.BindingViewHolder

class AllCreationStyleAdapter(val lastStyleId: String?) : PagingDataAdapter<CreationStyleInfo, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCreationStyleAllBinding.inflate(
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

    var itemVm: ItemCreationStyleVm? = null

    inner class ViewHolder(binding: ItemCreationStyleAllBinding) :
        BindingViewHolder<ItemCreationStyleAllBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemCreationStyleVm::class.java)
                binding.item = transVM.apply {
                    if (itemVm == null && item.styleId == lastStyleId) {
                        checked.set(true)
                        itemVm = this
                    }
                    onItemClick.value = {
                        if (item.isVip && !User.get().isVip) {
                            MembershipBenefitsActivity.start(
                                topActivity,
                                MembershipBenefitsActivity.Scene.InspirationCreationStyle
                            )
                        } else {
                            itemVm?.checked?.set(false)
                            this.checked.set(true)
                            itemVm = this
                        }
                    }
                }
                binding.executePendingBindings()
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CreationStyleInfo>() {
            override fun areItemsTheSame(
                oldItem: CreationStyleInfo,
                newItem: CreationStyleInfo
            ): Boolean {
                return oldItem.hashCode() == newItem.hashCode()
            }

            override fun areContentsTheSame(
                oldItem: CreationStyleInfo,
                newItem: CreationStyleInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}