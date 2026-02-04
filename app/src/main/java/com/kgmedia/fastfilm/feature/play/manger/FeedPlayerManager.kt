package com.kgmedia.fastfilm.feature.play.manger

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.kit.media.PlayerFactory
import com.kgmedia.fastfilm.widget.CoverPlayerView
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
            repeatMode = Player.REPEAT_MODE_OFF // 由业务层控制循环
            playWhenReady = true
        }
    }

    private var currentCoverPlayerView: CoverPlayerView? = null

    // [新] 记录当前播放的 ID，用于防止重复初始化
    var currentPlayingId: String? = null
        private set

    // [新] 记录用户是否主动暂停
    private var isUserPaused = false

    // 状态流
    private val _progressFlow = MutableStateFlow(Pair(0L, 0L))
    val progressFlow = _progressFlow.asStateFlow()

    // [修复1] 初始值设为 true (乐观模式)，默认假设是自动播放的，防止首帧闪烁
    private val _isPlayingFlow = MutableStateFlow(true)
    val isPlayingFlow = _isPlayingFlow.asStateFlow()

    sealed class PlayerEvent {
        object OnPlayEnded : PlayerEvent()
    }

    private val _playerEvent = MutableSharedFlow<PlayerEvent>()
    val playerEvent = _playerEvent.asSharedFlow()

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
//                _isPlayingFlow.value = isPlaying
                if (isPlaying) startProgressUpdater() else progressJob?.cancel()
            }
            // [修复2] 改用 playWhenReady 驱动 UI。
            // 只要 playWhenReady=true，就认为应该显示"播放中"状态(隐藏暂停图标)，忽略 Buffering 导致的 isPlaying=false
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                _isPlayingFlow.value = playWhenReady
//                if (playWhenReady) startProgressUpdater() else progressJob?.cancel()
            }
        })
    }

    fun attachAndPlay(track: WorkInfo, coverPlayerView: CoverPlayerView) {
        //再次强制同步状态，确保切视频瞬间图标是隐藏的
        _isPlayingFlow.value = true
        // 防止重复 attach 同一个视频导致画面闪烁
        if (currentPlayingId == track.workId && currentCoverPlayerView == coverPlayerView) {
            if (!exoPlayer.isPlaying && !isUserPaused) {
                exoPlayer.play()
            }
            return
        }

        isSeeking = false
        isUserPaused = false // 切视频重置暂停状态
        currentPlayingId = track.workId

        // 解绑旧 View
        if (currentCoverPlayerView != null && currentCoverPlayerView != coverPlayerView) {
            currentCoverPlayerView?.setPlayer(null)
        }
        currentCoverPlayerView = coverPlayerView

        // 绑定新 View
        coverPlayerView.setTrack(track)
        coverPlayerView.setPlayer(exoPlayer)

        val videoUrl = track.getPlayUrl() ?: return

        // [拓展] 构建带字幕的 MediaItem
        val mediaItem = buildMediaItem(track, videoUrl)

        val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()
        if (currentUri != videoUrl) {
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
        exoPlayer.play()
    }

    /**
     * [拓展点] 字幕/多音轨构建逻辑
     */
    private fun buildMediaItem(track: WorkInfo, url: String): MediaItem {
        val builder = MediaItem.Builder().setUri(url)

        // 示例：如果未来 TrackInfo 有 subtitleUrl 字段
        // track.subtitleUrl?.let { subUrl ->
        //     val subtitleConfig = SubtitleConfiguration.Builder(Uri.parse(subUrl))
        //         .setMimeType(MimeTypes.APPLICATION_SUBRIP) // .srt
        //         .setLanguage("zh")
        //         .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        //         .build()
        //     builder.setSubtitleConfigurations(listOf(subtitleConfig))
        // }

        return builder.build()
    }

    fun seekTo(position: Long) {
        isSeeking = true
        exoPlayer.seekTo(position)
        // 立即欺骗 UI 更新
        val validDuration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
        if (validDuration > 0) _progressFlow.value = Pair(position, validDuration)

        scope.launch {
            delay(800)
            isSeeking = false
            // 恢复后如果是暂停状态，手动触发一次更新确保位置正确
            if (!exoPlayer.isPlaying) {
                val current = exoPlayer.currentPosition
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                _progressFlow.emit(Pair(current, duration))
            }
        }
    }

    fun togglePlay() {
        if (exoPlayer.isPlaying) {
            isUserPaused = true
            pause()
        } else {
            isUserPaused = false
            resume()
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        // [修复] 只要不是已结束或空闲，就尝试播放（即使是缓冲中）
        if (exoPlayer.playbackState != Player.STATE_IDLE && exoPlayer.playbackState != Player.STATE_ENDED) {
            exoPlayer.play()
        } else if (exoPlayer.playbackState == Player.STATE_ENDED) {
            // 如果已结束，重新播放
            replay()
        }
    }

    // 给 Activity onResume 用，遵守用户暂停意愿
    fun resumeOnForeground() {
        if (!isUserPaused) {
            resume()
        }
    }

    fun replay() {
        isSeeking = false
        isUserPaused = false
        exoPlayer.seekTo(0)
        exoPlayer.play()
        _isPlayingFlow.value = true // Replay 时强制更新状态
    }

    @OptIn(UnstableApi::class)
    fun preload(url: String?) {
        if (url.isNullOrEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val dataSpec = DataSpec(url.toUri(), 0, 1024 * 1024, null)
                val dataSource = PlayerFactory.cacheDataSourceFactory.createDataSource()
                dataSource.open(dataSpec)
                dataSource.close()
            } catch (e: Exception) {
            }
        }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying && !isSeeking) {
                    val current = exoPlayer.currentPosition
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                    _progressFlow.emit(Pair(current, duration))
                }
                delay(200)
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