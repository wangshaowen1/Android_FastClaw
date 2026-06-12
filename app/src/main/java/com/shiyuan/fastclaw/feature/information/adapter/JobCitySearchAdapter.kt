package com.shiyuan.fastclaw.feature.information.adapter

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.CityLocation
import com.shiyuan.fastclaw.databinding.ItemJobCitySearchBinding

/**
 * 城市搜索结果适配器。
 */
class LocationSearchAdapter(
    private val selectedLocations: LinkedHashMap<String, CityLocation>,
    private val keywordProvider: () -> String,
    private val onItemClick: (CityLocation) -> Unit,
) : SingleBindingAdapter<ItemJobCitySearchBinding, CityLocation>(
    layoutId = R.layout.item_job_city_search,
    mData = emptyList(),
    onBind = { binding, _, item ->
        val selected = selectedLocations.containsKey(item.regionCode.orEmpty())
        val normalColor = ContextCompat.getColor(binding.root.context, R.color.color_90_000000)
        val selectedColor = ContextCompat.getColor(binding.root.context, R.color.color_2f6bff)
        binding.tvName.text = highlightSearchKeyword(item.regionName.orEmpty(), keywordProvider(), selectedColor)
        binding.tvName.setTextColor(if (selected) selectedColor else normalColor)
        binding.root.setOnSingleClickListener { onItemClick(item) }
    }
)

private fun highlightSearchKeyword(
    text: String,
    keyword: String,
    highlightColor: Int,
): CharSequence {
    val trimmedKeyword = keyword.trim()
    if (text.isEmpty() || trimmedKeyword.isEmpty()) return text
    val builder = SpannableStringBuilder(text)
    var startIndex = text.indexOf(trimmedKeyword, ignoreCase = true)
    while (startIndex >= 0) {
        builder.setSpan(
            ForegroundColorSpan(highlightColor),
            startIndex,
            startIndex + trimmedKeyword.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        startIndex = text.indexOf(trimmedKeyword, startIndex + trimmedKeyword.length, ignoreCase = true)
    }
    return builder
}
