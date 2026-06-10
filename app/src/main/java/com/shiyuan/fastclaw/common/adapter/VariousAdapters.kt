package com.shiyuan.fastclaw.common.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.chad.library.adapter.base.BaseMultiItemAdapter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.nhsd.basic.item.QuickItemViewModel
import com.nhsd.basic.item.QuickViewHolder

/**
 * @Introduce: 单类型 DataBinding 适配器，使用 Lambda 回调处理绑定逻辑
 *
 * @param layoutId    布局资源 ID
 * @param onBind      绑定回调：(binding, position, item) -> Unit
 */
open class SingleBindingAdapter<B : ViewDataBinding, T>(
    @LayoutRes private val layoutId: Int,
    mData: List<T>,
    private val onBind: (binding: B, position: Int, item: T) -> Unit
) : BaseQuickAdapter<T, QuickViewHolder>(mData) {

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder =
        QuickViewHolder.create(layoutId, parent)

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: T?) {
        if (item == null) return
        val binding = DataBindingUtil.bind<B>(holder.itemView)
        binding?.let { onBind(it, position, item) }
    }
}



/**
 * @Introduce: 多类型布局提供者接口
 */
interface MultiTypeProvider<T> {
    fun getItemType(position: Int, item: T?): Int
    @LayoutRes fun getLayoutId(viewType: Int): Int
}

/**
 * @Introduce: 多类型 DataBinding 适配器，使用 Lambda 回调处理绑定逻辑
 */
open class MultiTypeBindingAdapter<T>(
    mData: List<T>,
    private val provider: MultiTypeProvider<T>,
    private val onBind: (binding: ViewDataBinding, position: Int, item: T) -> Unit
) : BaseQuickAdapter<T, QuickViewHolder>(mData) {

    override fun getItemViewType(position: Int, list: List<T>): Int {
        return provider.getItemType(position, getItem(position))
    }

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder {
        return QuickViewHolder.create(provider.getLayoutId(viewType), parent)
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: T?) {
        if (item == null) return
        val binding = DataBindingUtil.bind<ViewDataBinding>(holder.itemView)
        binding?.let { onBind(it, position, item) }
    }
}

/**
 * @Introduce: 多类型 DataBinding 适配器，简化版本，使用 Map 配置类型映射
 *
 * @param typeLayoutMap   视图类型到布局 ID 的映射
 * @param typeChecker     根据 item 返回对应的视图类型
 * @param onBind          绑定回调：(binding, position, item) -> Unit
 */
open class SimpleMultiTypeBindingAdapter<T>(
    mData: List<T>,
    private val typeLayoutMap: Map<Int, Int>,
    private val typeChecker: (item: T) -> Int,
    private val onBind: (binding: ViewDataBinding, position: Int, item: T) -> Unit
) : BaseQuickAdapter<T, QuickViewHolder>(mData) {

    override fun getItemViewType(position: Int, list: List<T>): Int {
        return typeChecker(getItem(position) ?: return 0)
    }

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder {
        val layoutId = typeLayoutMap[viewType] ?: typeLayoutMap.values.firstOrNull()
            ?: throw IllegalArgumentException("Unknown viewType: $viewType, typeLayoutMap is empty")
        return QuickViewHolder.create(layoutId, parent)
    }

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: T?) {
        if (item == null) return
        val binding = DataBindingUtil.bind<ViewDataBinding>(holder.itemView)
        binding?.let { onBind(it, position, item) }
    }
}

