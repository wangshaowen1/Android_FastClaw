package com.kgmedia.fastfilm.feature.play

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.kgmedia.fastfilm.common.ext.getVm
import com.kgmedia.fastfilm.core.network.apis.entity.TrackInfo
import com.kgmedia.fastfilm.databinding.ItemVideoFeedBinding
import com.kgmedia.fastfilm.widget.PlayerSeekbar
import com.nhsd.basic.item.BindingViewHolder

class VideoFeedAdapter : PagingDataAdapter<TrackInfo, VideoFeedAdapter.VideoViewHolder>(DIFF_CALLBACK) {

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

        val playerView by lazy {
            binding.playerView
        }

        val seekbar by lazy {
            binding.seekbar
        }

        override fun bind(position: Int) {
            getItem(position)?.let { item ->
                val transVM = binding.getVm(position, item, ItemVideoFeedVm::class.java)
                binding.item = transVM
                binding.executePendingBindings()
            }
        }

        fun updateProgress(current: Long, total: Long) {
            if (total > 0) {
                val progress = (current * 100 / total).toInt()
                binding.seekbar.currentPosition = current
            }
        }

//        fun bind(item: TrackInfo) {
//            // 1. 强制重置状态 (关键！)
//            // 必须先把封面图盖上，防止复用了上一个视频的 Surface 画面
//            coverImage.visibility = View.VISIBLE
//            coverImage.alpha = 1f
//
//            // 2. 加载封面
//            Glide.with(coverImage).load(item.musicCoverUrl()).into(coverImage)
//
//            // 3. 重置进度条
//            seekBar.progress = 0
//
//            // 注意：这里不要操作播放器！播放器操作由 Activity 的 ScrollListener 触发
//        }
//

    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TrackInfo>() {
            override fun areItemsTheSame(oldItem: TrackInfo, newItem: TrackInfo) = oldItem.trackId == newItem.trackId
            override fun areContentsTheSame(oldItem: TrackInfo, newItem: TrackInfo) = oldItem == newItem
        }
    }
}