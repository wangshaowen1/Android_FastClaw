package com.kgmedia.fastfilm.feature.play.manger

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import com.kgmedia.fastfilm.core.network.apis.entity.TrackInfo
import com.kgmedia.fastfilm.kit.media.PlayerFactory
import com.kgmedia.fastfilm.widget.CoverPlayerView
import com.nhsd.basic.ext.launchNoCrash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 短剧播放管理器
 * 职责：
 * 1. 维护单例 ExoPlayer
 * 2. 处理播放器与 UI (CoverPlayerView) 的绑定与解绑
 * 3. 预加载与状态分发
 */
class FeedPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    // 全局单例播放器 (复用 PlayerFactory 的缓存配置)
    private val exoPlayer: ExoPlayer by lazy {
        PlayerFactory.createPlayer(withCache = true).apply {
            repeatMode = Player.REPEAT_MODE_ONE // 短剧默认单集循环
            playWhenReady = true
        }
    }

    // 当前绑定的 View
    private var currentCoverPlayerView: CoverPlayerView? = null

    // --- 状态流 ---
    private val _progressFlow = MutableStateFlow(Pair(0L, 0L))
    val progressFlow = _progressFlow.asStateFlow()

    sealed class PlayerEvent {
        object OnPlayEnded : PlayerEvent()
    }

    private val _playerEvent = MutableSharedFlow<PlayerEvent>()
    val playerEvent = _playerEvent.asSharedFlow()

    private var progressJob: Job? = null

    init {
        // 全局监听播放结束事件
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    scope.launch { _playerEvent.emit(PlayerEvent.OnPlayEnded) }
                }
            }
        })
    }

    /**
     * 核心：将播放器挂载到指定的 Item View 上
     */
    fun attachAndPlay(track: TrackInfo, coverPlayerView: CoverPlayerView) {
        // 1. 如果切换了 View，先解绑旧的
        if (currentCoverPlayerView != null && currentCoverPlayerView != coverPlayerView) {
            currentCoverPlayerView?.setPlayer(null)
        }
        currentCoverPlayerView = coverPlayerView

        // 2. 将数据传给 View (用于封面显示等逻辑)
        coverPlayerView.setTrack(track)

        // 3. 绑定播放器 (CoverPlayerView 内部会自动处理 Surface 和 Listener)
        coverPlayerView.setPlayer(exoPlayer)

        // 4. 设置媒体源
        val mediaItem = MediaItem.fromUri(track.musicPlayUrl().toString())

        // 只有切集时才 reset，如果是同一集则保持状态
        val isSameMedia = exoPlayer.currentMediaItem?.localConfiguration?.uri.toString() == track.musicPlayUrl()
        if (!isSameMedia) {
            exoPlayer.setMediaItem(mediaItem)
            // 如果有断点续播需求，在这里 seekTo
            // if (track.lastPosition > 0) exoPlayer.seekTo(track.lastPosition)
            exoPlayer.prepare()
        }

        exoPlayer.play()
        startProgressUpdater()
    }

    /**
     * 预加载下一集 (利用 CacheDataSource)
     */
    fun preload(url: String) {
        scope.launchNoCrash {
            try {
                withContext(Dispatchers.IO) {
                    val dataSpec = DataSpec(android.net.Uri.parse(url), 0, 1024 * 1024, null) // 预加载 1MB
                    val dataSource = PlayerFactory.cacheDataSourceFactory.createDataSource()
                    dataSource.open(dataSpec)
                    dataSource.close()
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _progressFlow.emit(Pair(exoPlayer.currentPosition, exoPlayer.duration))
                }
                delay(500)
            }
        }
    }

    fun pause() {
        exoPlayer.pause()
        progressJob?.cancel()
    }

    fun resume() {
        if (exoPlayer.playbackState == Player.STATE_READY) {
            exoPlayer.play()
            startProgressUpdater()
        }
    }

    fun replay() {
        exoPlayer.seekTo(0)
        exoPlayer.play()
    }

    fun release() {
        progressJob?.cancel()
        currentCoverPlayerView?.setPlayer(null)
        currentCoverPlayerView = null
        exoPlayer.release()
    }
}