package com.shiyuan.fastclaw.feature.information.adapter

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.SparseArray
import android.util.TypedValue
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.ItemIyCyPnBinding
import com.shiyuan.fastclaw.databinding.ItemIyPnBinding
import com.shiyuan.fastclaw.databinding.ItemIySearchBinding
import com.shiyuan.fastclaw.databinding.ItemJobIyCyPnBinding
import com.shiyuan.fastclaw.databinding.ItemSelectedBinding
import kotlin.collections.distinctBy
import kotlin.collections.plus

private fun highlightSearchKeyword(text: String?, keyword: String): CharSequence {
    if (text.isNullOrEmpty() || keyword.isEmpty()) return text.orEmpty()
    val builder = SpannableStringBuilder(text)
    var startIndex = text.indexOf(keyword, ignoreCase = true)
    while (startIndex >= 0) {
        builder.setSpan(
            ForegroundColorSpan(Color.parseColor("#2F6BFF")),
            startIndex,
            startIndex + keyword.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        startIndex = text.indexOf(keyword, startIndex + keyword.length, ignoreCase = true)
    }
    return builder
}


class IyPnAdapter(
    private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null,type: Int
) : SingleBindingAdapter<ItemIyPnBinding, CommonEnumItem>(
    layoutId = R.layout.item_iy_pn,
    mData = emptyList(),
    onBind = { binding, position, item ->
        if(type == 0){
            binding.tvName.text = item.name+ ""
        }else{
            binding.tvName.text = item.firstCategoryName+ ""
        }
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
    private val onRemove: ((CommonEnumItem) -> Unit)? = null,type: Int
) : SingleBindingAdapter<ItemSelectedBinding, CommonEnumItem>(
    layoutId = R.layout.item_selected,
    mData = emptyList(),
    onBind = { binding, _, item ->
        if(type == 0){
            binding.tvName.text = item.name+ ""
        }else{
            binding.tvName.text = item.positionName+ ""
        }
        binding.sl.setOnSingleClickListener {
            onRemove?.invoke(item)
        }
    }
)


/**
 * 已选选项适配器（原 createSelectedIyPnAdapter）
 */
class SearchAdapter(
    private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null,
    type: Int,
    private val keywordProvider: (() -> String)? = null
) : SingleBindingAdapter<ItemIySearchBinding, CommonEnumItem>(
    layoutId = R.layout.item_iy_search,
    mData = emptyList(),
    onBind = { binding, position, item ->
        if(type == 0){
            val keyword = keywordProvider?.invoke().orEmpty().trim()
            binding.tvName1.text = highlightSearchKeyword(item.name+"", keyword)
            binding.tvName2.text = highlightSearchKeyword( item.fullNamePath+ "", keyword)
        }else{
            val keyword = keywordProvider?.invoke().orEmpty().trim()
            binding.tvName1.text = highlightSearchKeyword(item.positionName+"", keyword)
            binding.tvName2.text = highlightSearchKeyword( item.categoryPath+ "", keyword)
        }
        binding.suItem.setOnSingleClickListener {
            onSelected?.invoke(item, position)
        }
    }
)
class IyPnSonAdapter( private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null,type: Int) : SingleBindingAdapter<ItemIyCyPnBinding, CommonEnumItem>(
    layoutId = R.layout.item_iy_cy_pn,
    mData = emptyList(),
    onBind = { binding, position, item ->
        if(type == 0){
            binding.tvName.text = item.name+ ""
        }else{
            binding.tvName.text = item.positionName+ ""
        }
        if (item.isSelected) {
            binding.suItem.setNormalColor(ContextCompat.getColor(binding.root.context, R.color.color_E8F1FF))
            binding.suItem.setBorderColor(ContextCompat.getColor(binding.root.context, R.color.color_2f6bff))
            binding.suItem.setBorderWidth(2)
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


class IyPnJobSonAdapter(
    private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null
) : SingleBindingAdapter<ItemJobIyCyPnBinding, CommonEnumItem>(
    layoutId = R.layout.item_job_iy_cy_pn,
    mData = emptyList(),
    onBind = { binding, _, item ->
        binding.tvName.text = item.secondCategoryName + ""
        val childAdapter = (binding.rcSon.getTag(R.id.rc_son) as? IyPnSonAdapter)
            ?: IyPnSonAdapter(onSelected, 1).also { adapter ->
                binding.rcSon.layoutManager = GridLayoutManager(
                    binding.root.context,
                    2,
                    RecyclerView.VERTICAL,
                    false
                )
                binding.rcSon.setHasFixedSize(true)
                binding.rcSon.isNestedScrollingEnabled = false
                binding.rcSon.itemAnimator = null
                binding.rcSon.adapter = adapter
                binding.rcSon.setTag(R.id.rc_son, adapter)
            }
        childAdapter.submitList(item.positions?.map { it })
    }
)


