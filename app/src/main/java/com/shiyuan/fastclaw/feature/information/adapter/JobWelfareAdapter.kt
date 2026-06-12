package com.shiyuan.fastclaw.feature.information.adapter

import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.ItemJobWelfareChipBinding
import com.shiyuan.fastclaw.databinding.ItemJobWelfareSelectedBinding

class JobWelfareAdapter(
    private val isSelected: (CommonEnumItem) -> Boolean,
    private val onSelected: (CommonEnumItem) -> Unit,
) : SingleBindingAdapter<ItemJobWelfareChipBinding, CommonEnumItem>(
    layoutId = R.layout.item_job_welfare_chip,
    mData = emptyList(),
    onBind = { binding, _, item ->
        val selected = isSelected(item)
        binding.tvName.text = item.name.orEmpty()
        binding.tvName.setTextColor(
            ContextCompat.getColor(
                binding.root.context,
                if (selected) R.color.color_2f6bff else R.color.color_90_000000
            )
        )
        binding.tvName.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        binding.tvName.background = ContextCompat.getDrawable(
            binding.root.context,
            if (selected) R.drawable.bg_welfare_chip_selected else R.drawable.bg_welfare_chip
        )
        binding.root.setOnSingleClickListener { onSelected(item) }
    }
)

class SelectedWelfareAdapter(
    private val onRemove: (CommonEnumItem) -> Unit,
) : SingleBindingAdapter<ItemJobWelfareSelectedBinding, CommonEnumItem>(
    layoutId = R.layout.item_job_welfare_selected,
    mData = emptyList(),
    onBind = { binding, _, item ->
        binding.tvName.text = item.name.orEmpty()
        binding.layoutItem.setOnSingleClickListener { onRemove(item) }
        binding.tvRemove.setOnSingleClickListener { onRemove(item) }
    }
)
