@file:JvmName("PlayerUtils")

package com.kgmedia.fastfilm.kit

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.kit.source.MediaSourceManager
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus


/**
 * 获取或创建与当前播放器绑定的 MediaSourceManager
 * 这会初始化 Manager 并绑定 Player，如果需要重置数据，后续需链式调用 setInitialMedias
 */
fun PlaylistPlayer.setupMediaSourceManager(initialMediasIsCache:Boolean=false): MediaSourceManager {
    return MediaSourceManager.with(this,initialMediasIsCache)
}
/**
 * 获取当前播放器绑定的 MediaSourceManager (如果存在)
 * 不会创建新实例，也不会重置状态
 */
fun PlaylistPlayer.getMediaSourceManager(): MediaSourceManager? {
    return MediaSourceManager.get(this)
}
fun PlaylistPlayer.resumePlay(shorts: List<ShortDramaInfo?>?) {
    if (shorts.isNullOrEmpty()) {
        return
    }
    playlist = shorts
    play()
}

fun PlaylistPlayer.resumePlay(
    shorts: List<ShortDramaInfo>?,
    playPosition: Int,
    positionMs: Long? = null
) {

    // 确定使用的播放列表
    val currentPlaylist = shorts ?: playlist ?: return // 如果播放列表为空，则直接返回
    // 检查播放位置是否有效
    if (playPosition < 0 || currentPlaylist.size <= playPosition) {
        return // 如果播放位置无效，直接返回
    }
    // 检查当前播放的曲目是否与指定位置的曲目相同
    val isSameShort =
        currentShort()?.equalsWithMedias(currentPlaylist.getOrNull(playPosition)) ?: false
    // 检查是否需要更新播放列表
    val playlistUpdated = playlist != currentPlaylist
    if (playlistUpdated) {
        playlist = shorts // 更新播放列表
    }
    // 跳转到指定的播放位置
    if (currentPlayingIndex != playPosition || playlistUpdated || !isSameShort) {
        skipTo(playPosition)
    }
    if (!isPlaying) {
        // 如果播放器不在播放，开始播放
        play()
    }

    if (playlistUpdated || !isSameShort) {
        // 定位到指定的时间戳
        seekTo(positionMs ?: 0)
    }


}


fun PlaylistPlayer.resumePlay(
    playPosition: Int,
    positionMs: Long? = null
) {
    resumePlay(null, playPosition, positionMs)
}

fun PlaylistPlayer.resumePlay(shorts: List<ShortDramaInfo>?, played: ShortDramaInfo?) {
    if (shorts.isNullOrEmpty() || played == null) {
        return
    }
    resumePlay(shorts, shorts.indexOf(played))
}

private val _shortsPlayer: DefaultPlaylistPlayer by lazy {
    DefaultPlaylistPlayer()
        .apply {
            playMode = PlaylistPlayer.PlayMode.NORMAL
            addListener(object : PlaylistListener {
                override fun onPlayingStatusChange(status: PlaylistListener.Status?) {
                    super.onPlayingStatusChange(status)
                    if (status == PlaylistListener.Status.STOPPED) {
                        playMode = PlaylistPlayer.PlayMode.NORMAL
                    }
                }

            })

        }
}
val globalShortsPlayer: DetailLoadingPlaylistPlayer by lazy {
    DetailLoadingPlaylistPlayer(delegate = _shortsPlayer)
}


/**
 * 从播放列表删除，如果播放列表清空，停止播放
 */
fun removeFromPlaylist(shortDramaId: String?) {
    val player = globalShortsPlayer
    val playlist = player.playlist as? MutableList ?: return
    val currentShort = player.currentShort()
    playlist.removeAll { it.dramasId == shortDramaId }
    if (playlist.isEmpty()) {
        player.stop()
    } else if (currentShort?.dramasId == shortDramaId) {
        val playingIndex = player.currentPlayingIndex
        val skipToIndex = if (playingIndex < 0) {
            0
        } else if (playingIndex >= playlist.size) {
            playlist.size - 1
        } else {
            playingIndex
        }
        player.skipTo(skipToIndex)
    }
}

fun Player.setPlayUrl(url: String?) {
    val mediaItem = MediaItem.Builder().setUri(url).build()
    setMediaItem(mediaItem)
}

fun Player.setAudioUrls(audioUris: List<String>?) {
    val mediaItems = audioUris?.map { uri ->
        MediaItem.fromUri(uri)
    }
    mediaItems?.let {
        setMediaItems(it)
    }
}

fun Player.playAudioUrls(audioUris: List<String>?) {
    setAudioUrls(audioUris)
    prepare()
    play()
}
fun Player.play(url: String?) {
    setPlayUrl(url)
    prepare()
    play()
}

fun Player.isPlaying(url: String?): Boolean {
    // 获取当前正在播放的媒体项
    val currentMediaItem = currentMediaItem ?: return false
    val currentUri = currentMediaItem.requestMetadata.mediaUri ?: currentMediaItem.localConfiguration?.uri
    return currentUri.toString() == url && this.isPlaying
}

fun Player.isCurrentMediaUrl(url: String?): Boolean {
    val currentMediaItem = currentMediaItem ?: return false
    val currentUri = currentMediaItem.requestMetadata.mediaUri ?: currentMediaItem.localConfiguration?.uri

    // 比较URI，处理可能的null值和编码差异
    return when {
        url == null -> currentUri == null
        currentUri == null -> false
        else -> currentUri == url.toUri()
    }
}


fun Player.isCurrentMediaUrlIn(urls: List<String?>): Boolean {
    val currentMediaItem = currentMediaItem ?: return false
    val currentUri = currentMediaItem.requestMetadata.mediaUri ?: currentMediaItem.localConfiguration?.uri

    return urls.any { url ->
        when {
            url == null -> currentUri == null
            currentUri == null -> false
            else -> currentUri == url.toUri()
        }
    }
}
fun Player.isCurrentMediaUrls(urls: List<String>?, ignoreOrder: Boolean = false): Boolean {
    val mediaItemCount = mediaItemCount

    // 快速检查数量
    if (mediaItemCount != urls?.size) {
        return false
    }

    // 提取URL并比较
    val currentUrls = (0 until mediaItemCount).map { index ->
        val mediaItem = getMediaItemAt(index)
        mediaItem.requestMetadata.mediaUri?.toString()
            ?: mediaItem.localConfiguration?.uri?.toString()
    }

    return if (ignoreOrder) {
        // 使用集合进行高效比较
        currentUrls.toSet() == urls.toSet()
    } else {
        // 顺序比较
        currentUrls == urls
    }
}
fun pauseGlobalPlayer() {
    globalShortsPlayer.pause()
}

val ioScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) + SupervisorJob() }
fun DefaultPlaylistPlayer.resumePlay(short: ShortDramaInfo?) {
    playlist?.indexOf(short)?.let { resumePlay(it) }
}


fun ShortPlayer.play(short: ShortDramaInfo?): Boolean {
    Logger.i("play short:${short?.dramasName} current =" + this.currentShort()?.dramasName)
    setShort(short)
    if (short != null) {
        if (!isPlaying) {
            val isPaused = playbackState == Player.STATE_READY
            if (isPaused) {
                play()
            } else {
                playWhenReady = true
                prepare()
                return true
            }
        }
    } else if (isPlaying) {
        stop()
    }
    return false
}

fun DefaultPlaylistPlayer.updateShortInPlaylist(refreshedShort: ShortDramaInfo?) {
    refreshedShort?:return
    val index = playlist?.indexOf(refreshedShort)?.takeIf { it > 0 } ?: return
    val short = playlist?.getOrNull(index) ?: return

    if (currentShort() == short) {
        globalShortsPlayer.delegate.player.setShort(refreshedShort)
    }
}

fun pausePlayers(){
    if(globalShortsPlayer.isPlaying){
        globalShortsPlayer.pause()
    }

}

