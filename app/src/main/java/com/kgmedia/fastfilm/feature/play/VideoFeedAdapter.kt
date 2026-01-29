package com.kgmedia.fastfilm.feature.play

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.DramaInfo
import com.kgmedia.fastfilm.databinding.ItemVideoFeedBinding
import com.kgmedia.fastfilm.widget.PlayerSeekbar
import com.nhsd.basic.item.BindingViewHolder

class VideoFeedAdapter(private val interactionListener: InteractionListener) :
    PagingDataAdapter<DramaInfo, VideoFeedAdapter.VideoViewHolder>(DIFF_CALLBACK) {

    // 定义交互回调
    interface InteractionListener {
        fun onTogglePlay(position: Int)
        fun onSeek(position: Int, progress: Long)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(
            ItemVideoFeedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(position)
    }

    // 可以在这里处理 payload 局部刷新 (例如点赞)
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            // 处理局部刷新
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class VideoViewHolder(binding: ItemVideoFeedBinding) : BindingViewHolder<ItemVideoFeedBinding>(binding) {

        init {
            binding.playerView.setOnClickListener {
                interactionListener.onTogglePlay(bindingAdapterPosition)
            }

            // 2. 进度条拖拽监听
            binding.seekbar.onSeekListener = object : PlayerSeekbar.OnSeekListener {
                override fun onDrag() {
                    // 拖拽开始：PlayerSeekbar 内部会自动处理 dragging 状态，
                    // 此时即使 updateProgress 被调用，PlayerSeekbar 也会优先显示 dragValue
                    // 所以这里可以不用做特殊逻辑，或者用于暂停自动隐藏控件的计时器
                }

                override fun onDragEnd(position: Long) {
                    // 拖拽结束：执行 Seek 操作
                    interactionListener.onSeek(bindingAdapterPosition, position)
                }
            }
        }

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemVideoFeedVm::class.java)
                binding.item = transVM
                binding.executePendingBindings()

                // [补充] 初始设置数据给 PlayerView，防止 DataBinding 延迟
                binding.playerView.setTrack(item)
                binding.seekbar.currentPosition = 0
                binding.seekbar.duration = item.getDuring() ?: 0
                binding.btnPause.visibility = View.GONE
            }
        }

        fun updateProgress(current: Long, total: Long) {
            if (total > 0) {
                binding.seekbar.duration = total
                binding.seekbar.currentPosition = current
            }
        }

        // 更新播放/暂停图标状态
        fun updatePlayState(isPlaying: Boolean) {
            binding.btnPause.visibility = if (isPlaying) View.GONE else View.VISIBLE
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DramaInfo>() {
            override fun areItemsTheSame(oldItem: DramaInfo, newItem: DramaInfo) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: DramaInfo, newItem: DramaInfo) = oldItem == newItem
        }
    }
}