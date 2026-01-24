package com.kgmedia.fastfilm.kit

import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

class DetailLoadingPlaylistPlayer(
    val delegate: DefaultPlaylistPlayer
) : PlaylistPlayer by delegate {

    private val loadingScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentLoadingJob: Job? = null

    // 不再需要手动更新播放列表，因为 MediaSourceManager 会处理

    override fun play() {
        Logger.d("DetailLoadingPlayer", "start play")

        // 取消前一个加载任务
        currentLoadingJob?.cancel()
        currentLoadingJob = null
        val currentShort = delegate.currentShort() ?: run {
            notifyError()
            return
        }

        if (currentShort.shotSegmentList != null) {
            // 已有详情数据，直接播放
            delegate.play()
        } else {
            // 启动新的加载任务
            currentLoadingJob = loadingScope.launch {
                loadAndPlay(currentShort)
            }
        }
    }

    override fun skipTo(index: Int) {
        // 取消前一个加载任务
        currentLoadingJob?.cancel()
        currentLoadingJob = null
        Logger.d("DetailLoadingPlayer", "skipTo $index")
        val targetShort = delegate.playlist?.getOrNull(index) ?: run {
            delegate.skipTo(index)
            return
        }
        delegate.skipTo(index)
    }

    private suspend fun loadAndPlay(shortInfo: ShortDramaInfo) {
        coroutineContext.ensureActive()
        notifyLoading()

        try {
            // 等待详情加载完成（MediaSourceManager 会自动更新播放列表）
            val detailedShort = waitForShortDetail(shortInfo.dramasId)
            coroutineContext.ensureActive()

            if (detailedShort != null) {
                // 直接播放，播放列表已经被 MediaSourceManager 更新
                withContext(Dispatchers.Main) {
                    delegate.play()
                }
                Logger.d("DetailLoadingPlayer", "加载成功 开始播放: ${shortInfo.dramasId}")


            } else {
                notifyError()
            }
        } catch (e: CancellationException) {
            Logger.d("DetailLoadingPlayer", "加载被取消: ${shortInfo.dramasId}")
            throw e
        } catch (e: TimeoutCancellationException) {
            Logger.e("DetailLoadingPlayer", "加载超时: ${shortInfo.dramasId}")
            notifyError()
        } catch (e: Exception) {
            Logger.e("DetailLoadingPlayer", "加载异常: ${shortInfo.dramasId}, ${e.message}")
            notifyError()
        } finally {
            currentLoadingJob = null
        }

        // 触发加载
        setupMediaSourceManager().ensurePlayableMedia(shortInfo.dramasId)
    }

    private suspend fun loadAndSkipTo(index: Int, shortInfo: ShortDramaInfo) {
        coroutineContext.ensureActive()
        notifyLoading()

        try {
            val detailedShort = waitForShortDetail(shortInfo.dramasId)
            coroutineContext.ensureActive()

            if (detailedShort != null) {
                // 直接跳转，播放列表已经被 MediaSourceManager 更新
                withContext(Dispatchers.Main) {
                    delegate.skipTo(index)
                }
            } else {
                notifyError()
            }
        } catch (e: CancellationException) {
            Logger.d("DetailLoadingPlayer", "跳转加载被取消: ${shortInfo.dramasId}")
            throw e
        } catch (e: TimeoutCancellationException) {
            Logger.e("DetailLoadingPlayer", "跳转加载超时: ${shortInfo.dramasId}")
            notifyError()
        } catch (e: Exception) {
            Logger.e("DetailLoadingPlayer", "跳转加载异常: ${shortInfo.dramasId}, ${e.message}")
            notifyError()
        } finally {
            currentLoadingJob = null
        }

        setupMediaSourceManager().ensurePlayableMedia(shortInfo.dramasId)
    }

    private suspend fun waitForShortDetail(dramaId: String): ShortDramaInfo? {
        return withTimeout(15000) {
            setupMediaSourceManager().preparedMediaDetails
                .map { it[dramaId] }
                .first { it != null }
        }
    }

    private fun notifyLoading() {
        delegate.playerlistListeners.forEach {
            it?.onPlayingStatusChange(PlaylistListener.Status.BUFFERING)
        }
    }

    private fun notifyError() {
        delegate.playerlistListeners.forEach { it?.onShortStreamError() }
    }

    override fun release() {
        currentLoadingJob?.cancel()
        currentLoadingJob = null
        loadingScope.cancel()
        delegate.release()
    }

    override fun pause() {
        Logger.d("DetailLoadingPlayer", "call pause ，current index=$currentPlayingIndex")
        delegate.pause()
    }
}