package com.shiyuan.fastclaw.feature.resume.preference

import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.CityLocation
import com.shiyuan.fastclaw.databinding.ItemJobCitySelectedBinding

/**
 * 底部已选城市横滑标签适配器。
 * 整体和右上角删除图标都可以触发移除。
 */
class JobCitySelectedAdapter(
    private val onRemove: (CityLocation) -> Unit,
) : SingleBindingAdapter<ItemJobCitySelectedBinding, CityLocation>(
    layoutId = R.layout.item_job_city_selected,
    mData = emptyList(),
    onBind = { binding, _, item ->
        // 城市名
        binding.tvName.text = item.regionName.orEmpty()
        // 整行点击：移除该城市
        binding.layoutItem.setOnSingleClickListener { onRemove(item) }
        // 关闭图标点击：移除该城市
        binding.tvRemove.setOnSingleClickListener { onRemove(item) }
    }
)
