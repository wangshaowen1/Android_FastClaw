package com.shiyuan.fastclaw.feature.information.viewHolder

import android.content.Context
import android.graphics.Color
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

    fun submitList(list: List<CityLocation>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
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
            itemTextView.bind(item, selectedLocations.containsKey(item.regionCode.orEmpty()))
            itemView.setOnSingleClickListener {
                onItemClick(item)
            }
        }
    }

    inner class SearchResultView(context: Context) : AppCompatTextView(context) {
        fun bind(item: CityLocation, selected: Boolean) {
            text = item.regionName.orEmpty()
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.color_2f6bff else R.color.color_90_000000
                )
            )
            setBackgroundColor(Color.TRANSPARENT)
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