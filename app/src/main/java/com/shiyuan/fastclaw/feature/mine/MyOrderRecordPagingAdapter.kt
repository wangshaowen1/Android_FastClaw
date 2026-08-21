package com.shiyuan.fastclaw.feature.mine

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.nhsd.basic.item.BindingViewHolder
import com.shiyuan.fastclaw.databinding.ItemMyOrderRecordBinding
import com.shiyuan.fastclaw.feature.mine.fragment.ItemMyOrderRecordVM
import com.shiyuan.fastclaw.feature.mine.fragment.MyOrderRecord

class MyOrderRecordPagingAdapter(
    private val onCopyClick: (String) -> Unit
) : PagingDataAdapter<MyOrderRecord, MyOrderRecordPagingAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMyOrderRecordBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(
        private val binding: ItemMyOrderRecordBinding
    ) : BindingViewHolder<ItemMyOrderRecordBinding>(binding) {

        override fun bind(position: Int) {
            getItem(position)?.let { record ->
                binding.item = ItemMyOrderRecordVM(record).apply {
                    onItemClick.value = {
                        onCopyClick(record.orderNo)
                    }
                }
                binding.executePendingBindings()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MyOrderRecord>() {
            override fun areItemsTheSame(
                oldItem: MyOrderRecord,
                newItem: MyOrderRecord
            ): Boolean {
                return oldItem.orderNo.isNotEmpty() && oldItem.orderNo == newItem.orderNo
            }

            override fun areContentsTheSame(
                oldItem: MyOrderRecord,
                newItem: MyOrderRecord
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
