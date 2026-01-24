package com.kgmedia.fastfilm.kit

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.common.utils.Utils
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.kit.PlaylistPlayer.PlayMode
import com.nhsd.basic.ext.toArrayList
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 *
 * The ShortListPlayer class is responsible for managing the playback of a list of shorts.
 * It can accept an external ShortPlayer instance to handle playback.
 * When the external player is playing other short, the corresponding listeners will not be called. and isPlaying always return false
 *
 */
class DefaultPlaylistPlayer(
    val player: ShortPlayer = PlayerFactory.createShortPlayer()
) : PlaylistPlayer, ShortPlayer.OnShortChangeListener {
    private var currentPlayingPosition: Long = 0
    private var saveState: State? = null
    private var mUpdateTimeTask: Job? = null

    companion object {
        private const val PROGRESS_UPDATE_INTERVAL = 1000L
    }

    /**
     * playMode in the playlist
     */
    private var playMode: PlayMode = PlayMode.NORMAL
    private val scope: CoroutineScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob()) }


    init {

        player.also {
            it.addOnShortChangeListener(this)
            it.addListener(object : Player.Listener {

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    Logger.e("__player error: ${error.message}")
                    if (!inPlayList(player.currentShort())) {
                        Logger.i("__player short not in playlist  ignore onPlayerError")
                        return
                    }
                    _mPlayerListListeners.forEach { mPlayerEngineListener ->
                        mPlayerEngineListener?.onShortStreamError()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)

                    if (!inPlayList(player.currentShort())) {
                        Logger.i(App.TAG, "__player not in playlist, ignore onIsPlayingChanged")
                        return
                    }
                    Logger.i(
                        App.TAG,
                        "__player is playing change to $isPlaying player.playbackState=${player.playbackState}"
                    )

                    when {
                        isPlaying -> {
                            if (player.playbackState == Player.STATE_READY) {
                                notifyPlayingStatus(PlaylistListener.Status.STARTED)
                                Logger.i(App.TAG, "__player state started...")
                            }
                        }


                        else -> {
                            if (player.playbackState != Player.STATE_IDLE) {
                                notifyPlayingStatus(PlaylistListener.Status.PAUSED)
                                Logger.i(App.TAG, "__player state pause...")
                            }

                        }
                    }
                }


                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)

                    // 检查当前播放的曲目是否在播放列表中
                    val isCurrentShortInPlaylist = inPlayList(player.currentShort())
                    if (!isCurrentShortInPlaylist) {
                        Logger.i(App.TAG, "__player not in playlist, ignore onPlaybackStateChanged")
                        return
                    }

                    when (playbackState) {
                        Player.STATE_READY -> Logger.i(App.TAG, "__player STATE_READY")

                        Player.STATE_BUFFERING -> {
                            Logger.i(App.TAG, "__player STATE_BUFFERING")
                            notifyPlayingStatus(PlaylistListener.Status.BUFFERING)
                        }

                        Player.STATE_ENDED -> {
                            Logger.i(
                                App.TAG,
                                "__player STATE_ENDED, is playing: ${player.isPlaying}"
                            )
                            handlePlaybackEnded()
                        }

                        Player.STATE_IDLE -> {
                            Logger.i(App.TAG, "__player STATE_IDLE")
//                            ManualStatus.update(ManualStatus.Status.STOPPED)
                            notifyPlayingStatus(PlaylistListener.Status.STOPPED)
                        }
                    }
                }

                private fun handlePlaybackEnded() {
                    saveState=null
                    val current = player.currentShort()
                    if (current?.dramasId == mPlaylist?.current()?.dramasId) {

                        val nextShort = mPlaylist?.autoNext(playMode)
                        if (nextShort != null) {
                            if (nextShort.dramasId == current?.dramasId) {
                                player.seekTo(0)
                            }
                            play()
                        } else {
                            stop()
                        }
                    }
                }


            })
        }
    }


    private fun notifyPlayingStatus(status: PlaylistListener.Status) {
        _mPlayerListListeners.forEach { l -> l?.onPlayingStatusChange(status) }
    }

    /**
     * Listener to the engine events
     */
    private var _mPlayerListListeners: HashSet<PlaylistListener?> = hashSetOf()

    val playerlistListeners: Set<PlaylistListener?>
        get() = _mPlayerListListeners.toSet()
    /**
     * Playlist
     */
    private var mPlaylist: Playlist? = null

    /**
     * Playlist of song played before
     */
    private var prevPlaylist: Playlist? = null


    override fun next(): Boolean {
        mPlaylist?.next()?.let {
            play()
            return true
        }
        return false
    }

    override fun setPlayMode(playMode: PlayMode) {
        this.playMode = playMode
    }

    override fun getPlayMode(): PlayMode {
        return playMode
    }

    override fun setPlaylist(playlist: List<ShortDramaInfo>?) {
        if (playlist?.isNotEmpty() == true) {
            prevPlaylist = mPlaylist
            mPlaylist = Playlist(playlist.toArrayList())
        } else mPlaylist = null
        if (!Utils.equals(playlist, prevPlaylist?.playList, playlist?.size ?: 0)) {
            saveState = null
            notifyPlaylistChanged(playlist)
        }
    }

    override fun addToPlaylist(short: ShortDramaInfo?) {
        short?.let {
            if (mPlaylist == null) {
                mPlaylist = Playlist(arrayListOf(short))
            } else if (mPlaylist?.playList?.contains(short) == false) {
                mPlaylist?.playList?.add(short)
            }

            notifyPlaylistChanged(mPlaylist?.playList)
        }
    }

    override fun addToPlaylist(playlist: List<ShortDramaInfo>?) {
        playlist?.let {
            if (mPlaylist == null) {
                mPlaylist = Playlist(playlist.toArrayList())
            } else
                if (mPlaylist?.playList?.containsAll(playlist) == false) {
                    mPlaylist?.playList?.addAll(playlist)
                }
            notifyPlaylistChanged(mPlaylist?.playList)
        }
    }

    private fun notifyPlaylistChanged(playlist: List<ShortDramaInfo>?) {
        _mPlayerListListeners.forEach {
            it?.onPlaylistChanged(playlist)
        }
    }

    override fun pause() {
        // check if we play, then pause
        // 如果处于缓冲状态(Buffering)，isPlaying可能为false，但 playWhenReady 是 true，此时必须强制暂停
        if (isPlaying || player.playWhenReady) {
            Logger.i("__player call pause in player audio engine.....")

            // 只有在真正有播放进度时才保存状态，避免刚缓冲就被存为0
            if (player.currentPosition > 0) {
                saveState = State(currentShort(), player.currentPosition)
            }

            mUpdateTimeTask?.cancel()
            mUpdateTimeTask = null

            // 核心动作：设置 playWhenReady = false
            player.pause()
        }
    }


    override fun play() {
        val currentShort = mPlaylist?.current() ?: run {
            Logger.i("__player current is null")
            _mPlayerListListeners.forEach { it?.onShortStreamError() }
            return
        }

        Logger.i("__player start play ${currentShort.dramasName}")

        if (mUpdateTimeTask == null || !mUpdateTimeTask!!.isActive) {
            mUpdateTimeTask = scope.launch {
                while (isActive) {
                    if (player.playbackState != Player.STATE_IDLE && isPlaying) {
                        currentPlayingPosition = player.currentPosition
                        _mPlayerListListeners.forEach { it?.onShortProgress(player.currentPosition) }
                    }
                    delay(PROGRESS_UPDATE_INTERVAL)
                }
            }
        }

//        if (_mPlayerListListeners.any { it?.onShortStart(currentShort) == false }) {
//            return
//        }

        if (!player.playWhenReady && player.playbackState == Player.STATE_READY && player.currentShort()?.dramasId == currentShort.dramasId) {
            Logger.i("__player resume short start...........")
            player.playWhenReady = true
            player.prepare()
            return
        }
        player.setShort(short = currentShort)
        if (saveState != null && saveState?.short?.dramasId == currentShort.dramasId) {
            saveState?.playbackPosition?.let { player.seekTo(it) }
        }
        player.playWhenReady = true
        player.prepare()
        Logger.i("short listener size=${_mPlayerListListeners.size}")
        _mPlayerListListeners.forEach { it?.onShortChanged(currentShort) }
        saveState = null
    }


    override fun currentShort(): ShortDramaInfo? {
        return mPlaylist?.current()
    }

    override fun getCurrentPlayingIndex(): Int {
        return mPlaylist?.getCurrentShortIndex() ?: 0
    }

    override fun playbackPosition(): Long {
        val savedPosition = saveState?.playbackPosition
        return if (player.isPlaying || savedPosition == null) {
            player.currentPosition
        } else {
            savedPosition
        }
    }

    override fun prev(): Boolean {
        mPlaylist?.prev()?.let {
            play()
            return true
        }
        return false
    }

    override fun skipTo(index: Int) {
        mPlaylist?.skipTo(index)
        Logger.i(App.TAG, "__player skipTo play${index}")
    }

    override fun addListener(playerlistListener: PlaylistListener?) {
        Logger.i("add listener=${playerlistListener}")
        playerlistListener?.let {
            _mPlayerListListeners.add(it)
        }
    }

    override fun removeListener(playerlistListener: PlaylistListener?) {
        playerlistListener?.let {
            _mPlayerListListeners.remove(it)
        }
    }

    override fun stop() {
        mUpdateTimeTask?.cancel() // 取消进度更新
        player.stop()
    }

    override fun release() {
        scope.cancel()
        _mPlayerListListeners.clear()
        player.release()
        player.removeOnShortChangeListener(this)
        playlist = null
    }


    override fun getPlaylist(): List<ShortDramaInfo>? {
        return mPlaylist?.playList
    }

    // 在类顶部添加状态追踪变量
    private var isSeeking = false // 是否正在执行 seek 操作
    private var shouldIndicatePlaying = false // 用户主动暂停前是否在播放状态

    // 修改 seekTo/forward/rewind 方法
    override fun seekTo(positionMs: Long) {
        isSeeking = true
        // 记录 seek 前的播放状态
        shouldIndicatePlaying = player.isPlaying
        player.seekTo(positionMs)
        saveState = State(currentShort(), positionMs)
        // 添加临时状态监听器
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isSeeking = false
                    player.removeListener(this)
                }
            }
        })

    }

    // 同步修改 forward 和 rewind
    override fun forward(time: Int) {
        seekTo(player.currentPosition + time)
    }

    override fun rewind(time: Int) {
        seekTo(player.currentPosition - time)
    }

    // 优化 isPlaying() 判断逻辑
    override fun isPlaying(): Boolean {
        return inPlayList(player.currentShort()) && when {
            // 正在执行 seek 且用户没有主动暂停 -> 维持播放状态
            isSeeking && shouldIndicatePlaying -> true
            // 正常播放状态 -> 透传底层状态
            else -> player.isPlaying
        }
    }


//    override fun forward(time: Int) {
//        seekJob?.cancel()
//        seekJob=scope.launch  {
//            player.seekTo(player.currentPosition + time)
//        }
//    }
//
//    override fun rewind(time: Int) {
//        seekJob?.cancel()
//        seekJob = scope.launch  {
//            player.seekTo(player.currentPosition - time)
//        }
//    }
//
//    override fun seekTo(positionMs: Long) {
//        Logger.i(MusicChatRoomImpl.LOG_TAG,"seek to $positionMs")
//        seekJob?.cancel()
//        seekJob = scope.launch {
//            player.seekTo(positionMs)
//        }
//
//    }

    override fun getDuration(): Long {
        return player.duration
    }

    override fun inPlayList(shortInfo: ShortDramaInfo?): Boolean {
        return playlist?.any { item -> item.dramasId == shortInfo?.dramasId } == true
    }

    override fun isPlaylistEnded(): Boolean {
        return mPlaylist?.playList?.isNotEmpty() == true &&mPlaylist?.current()==null
    }

    override fun restart() {
        skipTo(0)
        seekTo(0)
        play()
    }

    override fun prevList(): Boolean {
        prevPlaylist?.playList?.let {
            saveState=null
            playlist = it
            play()
            return true
        }
        return false
    }


    override fun onShortChanged(
        lastState: State?,
        short: ShortDramaInfo?
    ) {
        if (playlist?.isNotEmpty() == true && !inPlayList(short)) {
            Logger.i("__player short not in playlist ")
            if (saveState == null) {
                notifyPlayingStatus(PlaylistListener.Status.PAUSED)
                saveState = if (lastState?.short?.dramasId == currentShort()?.dramasId) {
                    State(currentShort(), lastState?.playbackPosition)
                } else {
                    State(currentShort(), currentPlayingPosition)
                }
            }
            return
        }
    }

}
