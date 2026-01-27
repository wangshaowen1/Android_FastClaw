package com.kgmedia.fastfilm.feature.play.manger

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import com.kgmedia.fastfilm.core.network.apis.entity.TrackInfo
import com.kgmedia.fastfilm.kit.media.PlayerFactory
import com.kgmedia.fastfilm.widget.CoverPlayerView
import com.nhsd.basic.utils.Logger
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

class FeedPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val exoPlayer: ExoPlayer by lazy {
        PlayerFactory.createPlayer(withCache = true).apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    private var currentCoverPlayerView: CoverPlayerView? = null

    // 状态流
    private val _progressFlow = MutableStateFlow(Pair(0L, 0L))
    val progressFlow = _progressFlow.asStateFlow()

    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow = _isPlayingFlow.asStateFlow()

    sealed class PlayerEvent {
        object OnPlayEnded : PlayerEvent()
    }

    private val _playerEvent = MutableSharedFlow<PlayerEvent>()
    val playerEvent = _playerEvent.asSharedFlow()

    // [关键变量] 标记是否正在 Seek
    private var isSeeking = false
    private var progressJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    scope.launch { _playerEvent.emit(PlayerEvent.OnPlayEnded) }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlayingFlow.value = isPlaying
                if (isPlaying) {
                    startProgressUpdater()
                } else {
                    progressJob?.cancel()
                }
            }
        })
    }

    fun attachAndPlay(track: TrackInfo, coverPlayerView: CoverPlayerView) {
        // 切换视频时重置 Seek 状态
        isSeeking = false

        if (currentCoverPlayerView != null && currentCoverPlayerView != coverPlayerView) {
            currentCoverPlayerView?.setPlayer(null)
        }
        currentCoverPlayerView = coverPlayerView

        coverPlayerView.setTrack(track)
        coverPlayerView.setPlayer(exoPlayer)

        val videoUrl = track.playUrl ?: return
        val mediaItem = MediaItem.fromUri(videoUrl)

        val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()
        if (currentUri != videoUrl) {
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

        exoPlayer.play()
    }

    fun togglePlay() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        if (exoPlayer.playbackState == Player.STATE_READY) {
            exoPlayer.play()
        }
    }

    fun replay() {
        isSeeking = false
        exoPlayer.seekTo(0)
        exoPlayer.play()
    }

    /**
     * 核心 Seek 逻辑修复
     */
    fun seekTo(position: Long) {
        Logger.i("FeedPlayerManager", "seekTo: $position")

        // 1. 标记开始 Seek，这会阻止 startProgressUpdater 发射旧的进度
        isSeeking = true

        // 2. 执行物理 Seek
        exoPlayer.seekTo(position)

        // 3. 立即强制更新 UI 到目标位置 (防止 UI 闪烁回跳)
        // 获取当前有效的时长，如果播放器还没准备好时长，使用 0
        val validDuration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
        // 只有当时长有效时，才发射欺骗数据
        if (validDuration > 0) {
            _progressFlow.value = Pair(position, validDuration)
        }

        // 4. 延迟重置 Seek 状态，给播放器缓冲时间
        scope.launch {
            delay(1000) // 1秒缓冲保护期
            isSeeking = false
            // 恢复后如果是暂停状态，且用户seek了，需要手动触发一次更新确保最终位置正确
            if (!exoPlayer.isPlaying) {
                val current = exoPlayer.currentPosition
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                _progressFlow.emit(Pair(current, duration))
            }
        }
    }

    fun preload(url: String?) {
        if (url.isNullOrEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val dataSpec = DataSpec(android.net.Uri.parse(url), 0, 1024 * 1024, null)
                val dataSource = PlayerFactory.cacheDataSourceFactory.createDataSource()
                dataSource.open(dataSpec)
                dataSource.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                // [关键] 只有当不处于 Seeking 状态时，才更新进度
                if (exoPlayer.isPlaying && !isSeeking) {
                    val current = exoPlayer.currentPosition
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                    _progressFlow.emit(Pair(current, duration))
                }
                delay(200) // 200ms 刷新一次
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        currentCoverPlayerView?.setPlayer(null)
        currentCoverPlayerView = null
        exoPlayer.release()
    }
}