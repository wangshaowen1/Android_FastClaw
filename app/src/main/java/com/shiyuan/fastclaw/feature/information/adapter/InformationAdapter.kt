package com.shiyuan.fastclaw.feature.information.adapter

import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.ItemIyCyPnBinding
import com.shiyuan.fastclaw.databinding.ItemIyPnBinding
import com.shiyuan.fastclaw.databinding.ItemIySearchBinding
import com.shiyuan.fastclaw.databinding.ItemSelectedBinding

class IyPnAdapter(
    private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null
) : SingleBindingAdapter<ItemIyPnBinding, CommonEnumItem>(
    layoutId = R.layout.item_iy_pn,
    mData = emptyList(),
    onBind = { binding, position, item ->
        binding.tvName.text = item.name
        if (item.isSelected) {
            binding.suItemIyPn.setNormalColor(ContextCompat.getColor(binding.root.context, R.color.white))
        } else {
            binding.suItemIyPn.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.color_F4F4F4))
        }
        binding.suItemIyPn.setOnSingleClickListener {
            onSelected?.invoke(item, position)
        }
    }
)


/**
 * 已选选项适配器（原 createSelectedIyPnAdapter）
 */
class SelectedIyPnAdapter(
    private val onRemove: ((CommonEnumItem) -> Unit)? = null
) : SingleBindingAdapter<ItemSelectedBinding, CommonEnumItem>(
    layoutId = R.layout.item_selected,
    mData = emptyList(),
    onBind = { binding, _, item ->
        binding.tvName.text = item.name
        binding.icClose.setOnSingleClickListener {
            onRemove?.invoke(item)
        }
    }
)


/**
 * 已选选项适配器（原 createSelectedIyPnAdapter）
 */
class SearchAdapter(
    private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null
) : SingleBindingAdapter<ItemIySearchBinding, CommonEnumItem>(
    layoutId = R.layout.item_iy_search,
    mData = emptyList(),
    onBind = { binding, position, item ->
        binding.tvName1.text = item.name
        binding.tvName2.text = item.fullNamePath
        binding.suItem.setOnSingleClickListener {
            onSelected?.invoke(item, position)
        }
    }
)

class IyPnSonAdapter( private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null) : SingleBindingAdapter<ItemIyCyPnBinding, CommonEnumItem>(
    layoutId = R.layout.item_iy_cy_pn,
    mData = emptyList(),
    onBind = { binding, position, item ->
        binding.tvName.text = item.name + ""
        if (item.isSelected) {
            binding.suItem.setNormalColor(ContextCompat.getColor(binding.root.context, R.color.color_E8F1FF))
            binding.suItem.setBorderColor(ContextCompat.getColor(binding.root.context, R.color.color_2f6bff))
            binding.suItem.setBorderWidth(1)
        } else {
            binding.suItem.setNormalColor(ContextCompat.getColor(binding.root.context, R.color.color_F4F4F4))
            binding.suItem.setBorderColor(ContextCompat.getColor(binding.root.context, R.color.color_F4F4F4))
            binding.suItem.setBorderWidth(0)
        }
        binding.suItem.setOnSingleClickListener {
            onSelected?.invoke(item, position)
        }
        val root = binding.root
        val lp = root.layoutParams
        if (lp is ViewGroup.MarginLayoutParams) {
            val dp5 = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                5f,
                root.context.resources.displayMetrics
            ).toInt()
            if (position % 2 == 0) {
                lp.leftMargin = 0
                lp.rightMargin = dp5
            } else {
                lp.leftMargin = dp5
                lp.rightMargin = 0
            }
            root.layoutParams = lp
        }
    }
)

