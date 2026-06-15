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
 * 展示当前搜索关键词命中的城市，并按已选状态切换文本色。
 */
class LocationSearchAdapter(
    private val selectedLocations: LinkedHashMap<String, CityLocation>,
    private val keywordProvider: () -> String,
    private val onItemClick: (CityLocation) -> Unit,
) : SingleBindingAdapter<ItemJobCitySearchBinding, CityLocation>(
    layoutId = R.layout.item_job_city_search,
    mData = emptyList(),
    onBind = { binding, _, item ->
        // 命中已选集合时按选中色显示
        val selected = selectedLocations.containsKey(item.regionCode.orEmpty())
        val normalColor = ContextCompat.getColor(binding.root.context, R.color.color_90_000000)
        val selectedColor = ContextCompat.getColor(binding.root.context, R.color.color_2f6bff)
        // 城市名按当前搜索关键词高亮
        binding.tvName.text = highlightSearchKeyword(item.regionName.orEmpty(), keywordProvider(), selectedColor)
        binding.tvName.setTextColor(if (selected) selectedColor else normalColor)
        // 点击切换选中
        binding.root.setOnSingleClickListener { onItemClick(item) }
    }
)

/**
 * 把搜索关键词在 text 中所有出现位置（忽略大小写）按指定颜色高亮。
 *
 * @param text 原始文本。
 * @param keyword 搜索关键词。
 * @param highlightColor 高亮颜色。
 * @return 带高亮 span 的 CharSequence。
 */
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
