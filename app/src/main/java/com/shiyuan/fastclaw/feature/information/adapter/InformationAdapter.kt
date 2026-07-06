package com.shiyuan.fastclaw.feature.information.adapter

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
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

/**
 * 把搜索关键词在 text 中所有出现位置（忽略大小写）染色。
 *
 * @param text 原始文本。
 * @param keyword 搜索关键词。
 * @return 带高亮 span 的 CharSequence。
 */
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


/**
 * 一级行业 / 一级职位 列表适配器。
 *
 * type = 0：行业（用 name）。
 * type = 1：职位（用 firstCategoryName）。
 */
class IyPnAdapter(
    private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null,type: Int
) : SingleBindingAdapter<ItemIyPnBinding, CommonEnumItem>(
    layoutId = R.layout.item_iy_pn,
    mData = emptyList(),
    onBind = { binding, position, item ->
        if(type == 0){
            // 行业：使用 name 字段
            binding.tvName.text = item.name+ ""
        }else{
            // 职位：使用一级分类名
            binding.tvName.text = item.firstCategoryName+ ""
        }
        if (item.isSelected) {
            // 选中态：白底
            binding.suItemIyPn.setNormalColor(ContextCompat.getColor(binding.root.context, R.color.white))
        } else {
            // 未选中态：灰底
            binding.suItemIyPn.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.color_F4F4F4))
        }
        // 点击切换
        binding.suItemIyPn.setOnSingleClickListener {
            onSelected?.invoke(item, position)
        }
    }
)


/**
 * 已选行业 / 已选职位 横滑标签适配器。
 *
 * type = 0：行业，使用 name。
 * type = 1：职位，使用 positionName。
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
        // 点击移除
        binding.sl.setOnSingleClickListener {
            onRemove?.invoke(item)
        }
    }
)


/**
 * 行业 / 职位 搜索结果列表适配器。
 * 根据当前搜索关键词高亮匹配文本（忽略大小写）。
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
            // 行业：name 和完整路径
            val keyword = keywordProvider?.invoke().orEmpty().trim()
            binding.tvName1.text = highlightSearchKeyword(item.name+"", keyword)
            binding.tvName2.text = highlightSearchKeyword( item.fullNamePath+ "", keyword)
        }else{
            // 职位：positionName 和分类路径
            val keyword = keywordProvider?.invoke().orEmpty().trim()
            binding.tvName1.text = highlightSearchKeyword(item.positionName+"", keyword)
            binding.tvName2.text = highlightSearchKeyword( item.categoryPath+ "", keyword)
        }
        // 点击选中
        binding.suItem.setOnSingleClickListener {
            onSelected?.invoke(item, position)
        }
    }
)

/**
 * 行业 / 职位选项适配器。
 * type = 0：行业页右侧二级网格，展示 name。
 * type = 1：职位页内层职位网格，展示 positionName。
 */
class IyPnSonAdapter(
    private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null,
    type: Int,
    private val isPlaceholder: ((CommonEnumItem) -> Boolean)? = null
) : SingleBindingAdapter<ItemIyCyPnBinding, CommonEnumItem>(
    layoutId = R.layout.item_iy_cy_pn,
    mData = emptyList(),
    onBind = bind@{ binding, position, item ->
        if (isPlaceholder?.invoke(item) == true) {
            binding.root.visibility = View.INVISIBLE
            binding.suItem.setOnClickListener(null)
            return@bind
        }
        binding.root.visibility = View.VISIBLE
        if(type == 0){
            binding.tvName.text = item.name+ ""
        }else{
            binding.tvName.text = item.positionName+ ""
        }
        if (item.isSelected) {
            // 选中态：蓝底 + 蓝边框 + 加粗
            binding.suItem.setNormalColor(ContextCompat.getColor(binding.root.context, R.color.color_E8F1FF))
            binding.suItem.setBorderColor(ContextCompat.getColor(binding.root.context, R.color.color_2f6bff))
            binding.suItem.setBorderWidth(2)
        } else {
            // 未选中态：灰底 + 无边框
            binding.suItem.setNormalColor(ContextCompat.getColor(binding.root.context, R.color.color_F4F4F4))
            binding.suItem.setBorderColor(ContextCompat.getColor(binding.root.context, R.color.color_F4F4F4))
            binding.suItem.setBorderWidth(0)
        }
        // 点击切换
        binding.suItem.setOnSingleClickListener {
            onSelected?.invoke(item, position)
        }
        // 行业页和职位页均为两列网格，按列设置左右间距。
        val root = binding.root
        val lp = root.layoutParams
        if (lp is ViewGroup.MarginLayoutParams) {
            val dp5 = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                5f,
                root.context.resources.displayMetrics
            ).toInt()
            if (position % 2 == 0) {
                // 左列：无左间距，有右间距
                lp.leftMargin = 0
                lp.rightMargin = dp5
            } else {
                // 右列：有左间距，无右间距
                lp.leftMargin = dp5
                lp.rightMargin = 0
            }
            root.layoutParams = lp
        }
    }
)


/**
 * 二级职位分组适配器：每个 item 是二级分类，内部嵌入三级职位网格。
 */
class IyPnJobSonAdapter(
    private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null
) : SingleBindingAdapter<ItemJobIyCyPnBinding, CommonEnumItem>(
    layoutId = R.layout.item_job_iy_cy_pn,
    mData = emptyList(),
    onBind = { binding, _, item ->
        // 二级分类名
        binding.tvName.text = item.secondCategoryName + ""
        // 用 Tag 缓存内层 RecyclerView 的适配器，避免重复创建
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
        // 渲染当前二级分类下的所有三级职位
        childAdapter.submitList(item.positions?.map { it })
    }
)
