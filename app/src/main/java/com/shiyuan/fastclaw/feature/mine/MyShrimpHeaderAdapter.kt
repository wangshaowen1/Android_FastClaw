package com.shiyuan.fastclaw.feature.mine

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shiyuan.fastclaw.databinding.ItemShrimpChipsHeaderBinding

/** 我的虾条头部
 * @createtime 2026年05月18日 17:33
 * @author TooWhiteT
 */

class MyShrimpHeaderAdapter :  RecyclerView.Adapter<MyShrimpHeaderAdapter.ViewHolder>() {

    private var points: String = "-"

    fun updatePoints(points: String) {
        this.points = points
        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemShrimpChipsHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(points)
    }

    override fun getItemCount() = 1

    inner class ViewHolder(private val binding: ItemShrimpChipsHeaderBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(data: String) {
            binding.tvPoints.text = data
        }
    }
}
