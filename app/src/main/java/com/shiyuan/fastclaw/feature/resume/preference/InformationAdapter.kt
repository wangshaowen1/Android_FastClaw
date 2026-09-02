package com.shiyuan.fastclaw.feature.resume.preference

import android.graphics.Color
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.SparseArray
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.item.QuickViewHolder
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.ItemIyCyPnBinding
import com.shiyuan.fastclaw.databinding.ItemIyPnBinding
import com.shiyuan.fastclaw.databinding.ItemIySearchBinding
import com.shiyuan.fastclaw.databinding.ItemJobIyCyPnBinding
import com.shiyuan.fastclaw.databinding.ItemJobSecondCategoryHeaderBinding
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

private const val GRID_SPAN_COUNT = 2
private const val GRID_ITEM_SIDE_SPACE_DP = 5f

private fun ItemIyCyPnBinding.bindOptionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    tvName.text = title
    if (isSelected) {
        suItem.setNormalColor(ContextCompat.getColor(root.context, R.color.color_E8F1FF))
        suItem.setBorderColor(ContextCompat.getColor(root.context, R.color.color_2f6bff))
        suItem.setBorderWidth(2)
    } else {
        suItem.setNormalColor(ContextCompat.getColor(root.context, R.color.color_F4F4F4))
        suItem.setBorderColor(ContextCompat.getColor(root.context, R.color.color_F4F4F4))
        suItem.setBorderWidth(0)
    }
    suItem.setOnSingleClickListener { onClick() }
}

private fun View.updateTwoColumnMargins(column: Int) {
    val lp = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    val space = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        GRID_ITEM_SIDE_SPACE_DP,
        context.resources.displayMetrics
    ).toInt()
    if (column == 0) {
        lp.leftMargin = 0
        lp.rightMargin = space
    } else {
        lp.leftMargin = space
        lp.rightMargin = 0
    }
    layoutParams = lp
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
        val title = if (type == 0) item.name.orEmpty() else item.positionName.orEmpty()
        binding.bindOptionItem(title, item.isSelected) {
            onSelected?.invoke(item, position)
        }
        binding.root.updateTwoColumnMargins(position % GRID_SPAN_COUNT)
    }
)


/**
 * 职位页右侧扁平列表：二级分类标题和三级职位 item 共用同一个 RecyclerView。
 */
class JobRightAdapter(
    private val isHeader: (CommonEnumItem) -> Boolean,
    private val onSelected: ((CommonEnumItem, Int) -> Unit)? = null
) : BaseQuickAdapter<CommonEnumItem, QuickViewHolder>(emptyList()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 1
        private const val VIEW_TYPE_POSITION = 2
    }

    fun isHeaderPosition(position: Int): Boolean {
        return getItem(position)?.let(isHeader) == true
    }

    override fun getItemViewType(position: Int, list: List<CommonEnumItem>): Int {
        return if (isHeader(list[position])) VIEW_TYPE_HEADER else VIEW_TYPE_POSITION
    }

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_HEADER) {
            R.layout.item_job_second_category_header
        } else {
            R.layout.item_iy_cy_pn
        }
        return QuickViewHolder.create(layoutId, parent)
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: CommonEnumItem?) {
        item ?: return
        val binding = DataBindingUtil.bind<ViewDataBinding>(holder.itemView) ?: return
        if (isHeader(item)) {
            (binding as? ItemJobSecondCategoryHeaderBinding)?.let { bindHeader(it, item) }
        } else {
            (binding as? ItemIyCyPnBinding)?.let { bindPosition(it, position, item) }
        }
    }

    private fun bindHeader(binding: ItemJobSecondCategoryHeaderBinding, item: CommonEnumItem) {
        binding.tvName.text = item.secondCategoryName.orEmpty().ifBlank { item.name.orEmpty() }
    }

    private fun bindPosition(binding: ItemIyCyPnBinding, position: Int, item: CommonEnumItem) {
        binding.root.visibility = View.VISIBLE
        binding.bindOptionItem(item.positionName.orEmpty(), item.isSelected) {
            onSelected?.invoke(item, position)
        }
        binding.root.updateTwoColumnMargins(positionColumn(position))
    }

    private fun positionColumn(position: Int): Int {
        var count = 0
        for (index in position - 1 downTo 0) {
            val item = getItem(index) ?: continue
            if (isHeader(item)) break
            count++
        }
        return count % 2
    }
}


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
                binding.rcSon.setHasFixedSize(true)
                binding.rcSon.itemAnimator = null
                binding.rcSon.adapter = adapter
                binding.rcSon.setTag(R.id.rc_son, adapter)
            }
        // 渲染当前二级分类下的所有三级职位
        childAdapter.submitList(item.positions?.map { it })
    }
)
