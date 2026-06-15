package com.shiyuan.fastclaw.feature.information.adapter

import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.ItemJobWelfareChipBinding
import com.shiyuan.fastclaw.databinding.ItemJobWelfareSelectedBinding

/**
 * 福利偏好选择
 */
/**
 * 福利偏好候选网格适配器。
 * 根据当前选中态切换文字颜色、加粗和背景。
 */
class JobWelfareAdapter(
    private val isSelected: (CommonEnumItem) -> Boolean,
    private val onSelected: (CommonEnumItem) -> Unit,
) : SingleBindingAdapter<ItemJobWelfareChipBinding, CommonEnumItem>(
    layoutId = R.layout.item_job_welfare_chip,
    mData = emptyList(),
    onBind = { binding, _, item ->
        val selected = isSelected(item)
        // 名称
        binding.tvName.text = item.name.orEmpty()
        // 选中态：蓝色文字 + 加粗；未选中：默认色 + 正常字重
        binding.tvName.setTextColor(
            ContextCompat.getColor(
                binding.root.context,
                if (selected) R.color.color_2f6bff else R.color.color_90_000000
            )
        )
        binding.tvName.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        // 选中态：蓝底；未选中：灰底
        binding.tvName.background = ContextCompat.getDrawable(
            binding.root.context,
            if (selected) R.drawable.bg_welfare_chip_selected else R.drawable.bg_welfare_chip
        )
        // 点击切换
        binding.root.setOnSingleClickListener { onSelected(item) }
    }
)

/**
 * 底部已选福利横滑标签适配器。
 * 整体和右上角删除图标都可以触发移除。
 */
class SelectedWelfareAdapter(
    private val onRemove: (CommonEnumItem) -> Unit,
) : SingleBindingAdapter<ItemJobWelfareSelectedBinding, CommonEnumItem>(
    layoutId = R.layout.item_job_welfare_selected,
    mData = emptyList(),
    onBind = { binding, _, item ->
        // 名称
        binding.tvName.text = item.name.orEmpty()
        // 整行点击：移除
        binding.layoutItem.setOnSingleClickListener { onRemove(item) }
        // 关闭图标点击：移除
        binding.tvRemove.setOnSingleClickListener { onRemove(item) }
    }
)
