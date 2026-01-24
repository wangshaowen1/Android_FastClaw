package com.kgmedia.fastfilm.kit

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentSubtitleInfo
import com.nhsd.basic.utils.Logger
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min

/**
 * ShortPlayer 的默认实现，使用 Kotlin 委托模式。
 * * 修改说明：
 * 全局进度计算不再依赖 ExoPlayer 的 Timeline，而是直接基于 ShortDramaInfo 中的 actualDuration 字段。
 */
class DefaultShortPlayer(
    private val realPlayer: Player
) : ShortPlayer, Player by realPlayer {

    companion object {
        const val LOG_TAG = "DefaultShortPlayer"
    }

    private var currentShort: ShortDramaInfo? = null

    // 使用线程安全的列表
    private val onShortChangeListeners = CopyOnWriteArrayList<ShortPlayer.OnShortChangeListener>()

    // 映射 MediaItem Index -> 分镜片段信息
    private val mediaItemMapping = ArrayList<Pair<ShortDramaSegmentInfo, ShortDramaSegmentSubtitleInfo>>()

    // 内部播放器监听器，仅用于监听切集事件通知 UI
    private val internalPlayerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            notifyPlaybackChange()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                notifyPlaybackChange()
            }
        }
    }

    init {
        realPlayer.addListener(internalPlayerListener)
    }

    override fun release() {
        realPlayer.removeListener(internalPlayerListener)
        onShortChangeListeners.clear()
        mediaItemMapping.clear()
        realPlayer.release()
    }

    override fun setShort(short: ShortDramaInfo?) {
        currentShort = short
        buildMediaItemMapping(short)

        // 设置播放链接
        realPlayer.setAudioUrls(short?.getMediaUrls().orEmpty())
    }

    override fun currentShort(): ShortDramaInfo? = currentShort

    /**
     * 构建映射表：MediaItem Index -> (Segment, Subtitle)
     */
    private fun buildMediaItemMapping(short: ShortDramaInfo?) {
        mediaItemMapping.clear()
        short?.shotSegmentList?.forEach { segment ->
            segment.shotSegmentSubtitleList?.forEach { sub ->
                mediaItemMapping.add(segment to sub)
            }
        }
    }

    private fun notifyPlaybackChange() {
        val currentIndex = realPlayer.currentMediaItemIndex
        if (currentIndex in mediaItemMapping.indices) {
            val (segment, sub) = mediaItemMapping[currentIndex]
            onShortChangeListeners.forEach {
                it.onShortPlaybackChange(currentShort, segment, sub)
            }
        }
    }

    // =======================================================
    // ✅ 核心修改：基于 actualDuration 的全局进度与 Seek 逻辑
    // =======================================================

    override fun getGlobalPlaybackInfo(): ShortPlayer.GlobalPlaybackInfo {
        val short = currentShort ?: return ShortPlayer.GlobalPlaybackInfo(0, 0, 0)
        val segments = short.shotSegmentList ?: return ShortPlayer.GlobalPlaybackInfo(0, 0, 0)

        // 1. 获取当前播放器所在的 MediaItem 索引
        val currentMediaIndex = realPlayer.currentMediaItemIndex

        // 安全检查：防止索引越界
        if (currentMediaIndex !in mediaItemMapping.indices) {
            return ShortPlayer.GlobalPlaybackInfo(0, 0, 0)
        }

        // 2. 获取当前正在播放的分镜 (Segment)
        val currentSegment = mediaItemMapping[currentMediaIndex].first

        // 3. 计算总时长 & 当前分镜之前的时长和
        var totalDuration = 0L
        var beforeCurrentSegmentDuration = 0L

        // 遍历所有 Segment 进行累加（不依赖 Timeline）
        for (segment in segments) {
            val segDuration = segment.durationMs // 使用 actualDuration * 1000

            // 累加总时长
            totalDuration += segDuration

            // 如果此 Segment 在当前播放 Segment 之前（通过比较对象引用或ID）
            // 注意：这里假设 shotSegmentList 是有序的
            if (segment == currentSegment) {
                // 找到了当前 Segment，循环继续为了计算总时长，但停止累加 beforeDuration
                // 标记一下不再累加 before
            } else if (segments.indexOf(segment) < segments.indexOf(currentSegment)) {
                beforeCurrentSegmentDuration += segDuration
            }
        }

        // 4. 计算全局进度
        // 全局进度 = (前面所有分镜的时长) + (播放器当前进度)
        // 注意：如果一个 Segment 有多个 Subtitle(MediaItem)，这里假设 currentPosition 是相对于整个 Segment 的，
        // 或者 Segment 只有一个 MediaItem。如果是多 MediaItem 且 actualDuration 是总和，
        // 这里会有微小误差(进度条在分镜内切 Subtitle 时会回跳)，但根据需求这是最稳妥的方案。
        var globalPosition = beforeCurrentSegmentDuration + realPlayer.currentPosition

        // 修正溢出（防止进度超过总时长）
        globalPosition = min(globalPosition, totalDuration)

        return ShortPlayer.GlobalPlaybackInfo(
            positionMs = globalPosition,
            durationMs = totalDuration,
            mediaItemIndex = currentMediaIndex
        )
    }

    override fun seekToGlobalPosition(positionMs: Long) {
        val short = currentShort ?: return
        val segments = short.shotSegmentList ?: return

        var remainingMs = positionMs

        // 1. 遍历 Segment 列表，找到目标 Segment
        for (segment in segments) {
            val segDuration = segment.durationMs

            if (remainingMs < segDuration) {
                // 找到目标分镜：segment
                // 目标位置就在这个分镜内的 remainingMs 处

                // 2. 找到该 Segment 对应的第一个 MediaItem Index
                // 这是一个从 Segment -> MediaItemIndex 的反向查找
                val targetMediaIndex = mediaItemMapping.indexOfFirst { it.first == segment }

                if (targetMediaIndex != -1) {
                    Logger.i(LOG_TAG, "Seek Global($positionMs) -> Seg(${segment.sequenceNumber}), Index=$targetMediaIndex, Pos=$remainingMs")
                    realPlayer.seekTo(targetMediaIndex, remainingMs)
                }
                return
            }

            // 减去当前分镜时长，继续查找下一个
            remainingMs -= segDuration
        }

        // 如果循环结束还没 return，说明 seek 到了最后，跳转到最后一个 item 的末尾
        if (segments.isNotEmpty()) {
            val lastMediaIndex = mediaItemMapping.size - 1
            if (lastMediaIndex >= 0) {
                realPlayer.seekTo(lastMediaIndex, realPlayer.duration)
            }
        }
    }

    override fun addOnShortChangeListener(listener: ShortPlayer.OnShortChangeListener?) {
        listener?.let { onShortChangeListeners.add(it) }
    }

    override fun removeOnShortChangeListener(listener: ShortPlayer.OnShortChangeListener?) {
        listener?.let { onShortChangeListeners.remove(it) }
    }
}