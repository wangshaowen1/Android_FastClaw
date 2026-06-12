package com.shiyuan.fastclaw.feature.information.adapter

import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.CityLocation
import com.shiyuan.fastclaw.databinding.ItemJobCitySelectedBinding

/**
 * 底部已选城市适配器。
 */
class JobCitySelectedAdapter(
    private val onRemove: (CityLocation) -> Unit,
) : SingleBindingAdapter<ItemJobCitySelectedBinding, CityLocation>(
    layoutId = R.layout.item_job_city_selected,
    mData = emptyList(),
    onBind = { binding, _, item ->
        binding.tvName.text = item.regionName.orEmpty()
        binding.layoutItem.setOnSingleClickListener { onRemove(item) }
        binding.tvRemove.setOnSingleClickListener { onRemove(item) }
    }
)
