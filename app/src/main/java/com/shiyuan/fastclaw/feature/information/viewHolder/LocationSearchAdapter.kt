package com.shiyuan.fastclaw.feature.information.viewHolder

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.network.apis.response.CityLocation

/**
 * @desc：城市列表搜索
 * @author：CaoWei
 * @date：2026/6/10 16:17
 */
class LocationSearchAdapter(var selectedLocations: LinkedHashMap<String, CityLocation>, private val onItemClick: (CityLocation) -> Unit) : RecyclerView.Adapter<LocationSearchAdapter.LocationViewHolder>() {

    private val items = mutableListOf<CityLocation>()
    private var keyword: String = ""

    fun submitList(list: List<CityLocation>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun setKeyWord(keyword: String = "") {
        this.keyword = keyword
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        return LocationViewHolder(SearchResultView(parent.context))
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class LocationViewHolder(val itemTextView: SearchResultView) : RecyclerView.ViewHolder(itemTextView) {
        fun bind(item: CityLocation) {
            itemTextView.bind(item, selectedLocations.containsKey(item.regionCode.orEmpty()), keyword)
            itemView.setOnSingleClickListener {
                onItemClick(item)
            }
        }
    }

    inner class SearchResultView(context: Context) : AppCompatTextView(context) {
        fun bind(item: CityLocation, selected: Boolean, keyword: String) {
            val cityName = item.regionName.orEmpty()
            val normalColor = ContextCompat.getColor(context, R.color.color_90_000000)
            val selectedColor = ContextCompat.getColor(context, R.color.color_2f6bff)
            text = createHighlightedName(cityName, keyword, selected, normalColor, selectedColor)
            setTextColor(if (selected) selectedColor else normalColor)
            setBackgroundColor(Color.TRANSPARENT)
        }

        private fun createHighlightedName(
            cityName: String,
            keyword: String,
            selected: Boolean,
            normalColor: Int,
            selectedColor: Int,
        ): CharSequence {
            val trimmedKeyword = keyword.trim()
            if (cityName.isEmpty() || trimmedKeyword.isEmpty()) return cityName
            val startIndex = cityName.indexOf(trimmedKeyword, ignoreCase = true)
            if (startIndex < 0) return cityName
            val endIndex = startIndex + trimmedKeyword.length
            return SpannableString(cityName).apply {
                setSpan(
                    ForegroundColorSpan(if (selected) selectedColor else normalColor),
                    0,
                    cityName.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    ForegroundColorSpan(selectedColor),
                    startIndex,
                    endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        init {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.resources.displayMetrics.density.let { (44 * it + 0.5f).toInt() }
            )
            gravity = Gravity.CENTER_VERTICAL
            textSize = 15f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
    }
}
