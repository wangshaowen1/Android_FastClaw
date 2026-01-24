package com.kgmedia.fastfilm.feature.play

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableLong
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.livedata.UnPeekLiveData
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.entity.DramasSeriesInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentSubtitleInfo
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.payment.points.PointsRechargeActivity
import com.kgmedia.fastfilm.feature.play.data.ShortPlayRepository
import com.kgmedia.fastfilm.feature.play.more.ShareShortDramaViewModel
import com.kgmedia.fastfilm.kit.DefaultShortPlayer
import com.kgmedia.fastfilm.kit.ShortPlayer
import com.kgmedia.fastfilm.kit.State
import com.kgmedia.fastfilm.kit.source.MediaSourceManager
import com.kgmedia.fastfilm.widget.PlayerSeekbar
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/**
 * 短剧播放
 */
class ShortDramaPlayerViewModel : ShareShortDramaViewModel<ShortPlayRepository>(),
    ShortPlayer.OnShortChangeListener {

    companion object {
        private const val LOG_TAG = "ShortDramaPlayerViewModel"
        private const val PLAYER_PREPARING_DELAY_MS = 1000L
    }

    var mToolbarVM = ToolbarViewModel(
        navigationDrawable = R.drawable.ic_nav_back.getDrawable(),
        navigationAction = { back() },
        title = "",
        background = Color.TRANSPARENT
    )

    // 状态管理
    val currentSegment: ObservableField<ShortDramaSegmentInfo> = ObservableField()
    val currentSegmentAudioDuration by lazy { ObservableInt(0) }
    val shortDramaSegments = MutableLiveData<List<ShortDramaSegmentInfo>>()
    val currentSegmentPosition = MutableLiveData<Int>()
    val isPlaying = MutableLiveData<Boolean>()
    val dramaDetail = MutableLiveData<ShortDramaInfo>()
    val dramasSeries = ObservableField<DramasSeriesInfo>()
    val isPaused = MediatorLiveData<Boolean>()

    val shortName = ObservableField<String>()
    val shortCover = ObservableField<String>()
    val shortIntro = ObservableField<String>()
    val shortStyle = ObservableField<String>()

    val isMyShorts = ObservableField(true)
    val isLocked = UnPeekLiveData<Boolean>()
    val autoUnlock = UnPeekLiveData<Boolean>()
    val autoUnlockSwitch = ObservableBoolean(false)
    val subtitles = ObservableField<String>()
    val unLockCoins = ObservableField<Int>()

    val isPreparingToPlay = MutableLiveData(false)

    val eyeProtectionEnabled = MutableLiveData(Config.get().eyeProtectionEnabled)

    val likeNumStr = ObservableField<String>()
    val followNumStr = ObservableField<String>()
    val isFollowed = ObservableField(false)
    val isLiked = ObservableField(false)
    val totalWorkNum = ObservableField<Int>()

    val unreleased by lazy { ObservableField(mBundle.getBoolean(Constants.Extra.UNRELEASED)) }
    private var positionMonitoringJob: Job? = null

    // 数据
    val dramaId by lazy { mBundle.getString(Constants.Extra.DRAMA_ID) }

    /**
     * 剧集播放标志，区分首页推荐列表用
     */
    val playerInSeries by lazy { mBundle.getBoolean(Constants.Extra.PLAYER_IN_SERIES) }
    var seriesId: String? = null
    val workModel by lazy {
        //true是作品模式（只看短剧详情）
        mBundle.getBoolean(Constants.Extra.MODE)
    }
    private var currentShortDramaInfo: ShortDramaInfo? = null
    private val playbackTasks = mutableListOf<Job>() // 所有播放任务

    private var resetPreparingStateTask: Job? = null
    private var isPauseInStop: Boolean = false

    /**
     * 当前播放进度ms (全局)
     */
    val currentPosition by lazy { ObservableLong(0) }

    /**
     * 总时长ms (全局)
     */
    val duration by lazy { ObservableLong(0) }

    /**
     * 拖动进度滑块
     */
    val seeking by lazy { ObservableBoolean(false) }

    val onSeekListener by lazy {
        object : PlayerSeekbar.OnSeekListener {
            override fun onDragEnd(position: Long) {
                if (isLocked.value == true) {
                    return
                }
                Logger.i(LOG_TAG, "onDragEnd seek to global position:$position")
                if (isLocked.value == true) return
                player?.seekToGlobalPosition(position)
                seeking.set(false)
            }

            override fun onDrag() {
                if (isLocked.value == true) {
                    return
                }
                seeking.set(true)
            }
        }
    }

    // --- 新增埋点相关变量 Start ---
    private var validViewDurationMs = 0L // 当前视频累计有效播放时长
    private var hasReportedEffectiveView = false // 是否已经上报过
    private var lastLoopTimestamp = 0L // 上一次轮询的时间戳
    // --- 新增埋点相关变量 End ---

    var player: ShortPlayer? = null
    var manager: MediaSourceManager? = null
    override val shortDrama: ShortDramaInfo?
        get() = currentShortDramaInfo

    override fun onCreate() {
        super.onCreate()
        startPreparedDetails()
        loadDramaDetail()
        player?.addListener(createPlayerListener())
        player?.addOnShortChangeListener(this)
        listOf(isPlaying, isPreparingToPlay).forEach { source ->
            isPaused.addSource(source) { updatePausedState() }
        }
        startPositionPolling()
    }

    private fun startPositionPolling() {
        positionMonitoringJob = launch {
            var lastCheckTime = System.currentTimeMillis()
            while (true) {
                delay(250)

                val now = System.currentTimeMillis()
                // 计算距离上次检查过去了多久 (理论上约等于 250ms)
                val delta = now - lastCheckTime
                // 更新锚点，确保下次计算是基于当前时间
                lastCheckTime = now

                val shortPlayer = player as? DefaultShortPlayer ?: continue
                if (shortPlayer.isPlaying && !seeking.get()) {
                    val info = shortPlayer.getGlobalPlaybackInfo()
                    currentPosition.set(info.positionMs)
                    duration.set(info.durationMs)

                    val playingId = shortPlayer.currentShort()?.dramasId
                    // 判断：只有当播放器播的 ID 等于我当前的 dramaId 时，才算我的有效时长
                    val isCurrentVideo = playingId == dramaId
                    if (isCurrentVideo && !hasReportedEffectiveView) {
                        // 过滤掉异常大的时间间隔（例如：切后台回来那一瞬间 delta 可能会很大，不应计入）
                        if (delta < 2000) {
                            validViewDurationMs += delta
                        }

                        // 3. 检查是否满足 15 秒
                        if (validViewDurationMs >= 15000) {
                            reportEffectiveViewEvent()
                        }
                        Logger.i(LOG_TAG, "有效播放满15s，埋点: $dramaId  累计时长 $validViewDurationMs")
                    }
                }

                lastLoopTimestamp = System.currentTimeMillis()
            }
        }
    }

    // --- 新增方法: 上报埋点 ---
    private fun reportEffectiveViewEvent() {
        hasReportedEffectiveView = true
//        val currentId = dramaId // 或者 player?.currentShort()?.dramasId
        Logger.i(LOG_TAG, "有效播放满15s，上报埋点: $dramaId")
        EventManager.onAppTrack(AppEvent.JG_COMIC_PLAY_PAGE_EFFECTIVE_VIEW_DURATION)
    }

    // --- 新增方法: 重置计数器 ---
    private fun resetTrackingStats() {
        if (validViewDurationMs > 0L) {
            validViewDurationMs = 0L
            hasReportedEffectiveView = false
            Logger.i(LOG_TAG, "有效播放满15s的埋点参数重置: $dramaId")
            lastLoopTimestamp = 0L // 归零，防止切集时把上一集的时间差算进来
        }
    }


    override fun onStart() {
        super.onStart()
        val isPaused = isPauseInStop &&
                (player?.playbackState == Player.STATE_READY || player?.playbackState == Player.STATE_BUFFERING)
                && player?.playWhenReady == false
        if (isPaused) {
            player?.play()
        }
    }

    override fun onStop() {
        super.onStop()
        // 只有当播放器里播的确实是我的内容时，才记录“暂停状态”，否则说明我已经滑过去了，不需要记录
        if (player?.currentShort()?.dramasId == dramaId) {
            isPauseInStop = isPlaying.value == true
            // 传入 true，表示“如果播放器已经切到下一集了，就别暂停了”
            pausePlayback(onlyIfCurrentContent = true)
        } else {
            // 我已经不是当前播放的内容了，只需要停止我内部的计时任务，不要碰播放器
            cancelPlaybackTasks()
            resetPlayStatus()
            isPauseInStop = false
        }
    }

    private fun createPlayerListener(): Player.Listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val currentPlayerDramaId = player?.currentShort()?.dramasId
            if (currentPlayerDramaId == dramaId) {
                this@ShortDramaPlayerViewModel.isPlaying.postValue(isPlaying)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (player?.currentShort()?.dramasId == dramaId) {
                if (playbackState == Player.STATE_READY) {
                    currentSegmentAudioDuration.set(player?.duration?.toInt() ?: 0)
                }
                // 【补充】当缓冲结束进入 Ready 状态时，再次确认一下播放状态，防止状态卡死
                if (playbackState == Player.STATE_READY && player?.isPlaying == true) {
                    this@ShortDramaPlayerViewModel.isPlaying.postValue(true)
                }
            }
        }
    }

    override fun onShortChanged(
        lastState: State?,
        short: ShortDramaInfo?
    ) {
        // 列表数据和详情数据在内存中可能是两个不同的对象，用 ID 最稳妥
        val isCurrentDrama = short?.dramasId == dramaId
        if (isCurrentDrama) {
            // 如果切到了我，立即同步当前播放器的状态
            isPlaying.value = player?.isPlaying ?: false
        } else {
            // 这比在 onIsPlayingChanged 里猜测要准确得多，且不会产生时序竞争
            if (isPlaying.value == true) {
                isPlaying.value = false
            }
        }
    }

    override fun onShortPlaybackChange(
        short: ShortDramaInfo?,
        segment: ShortDramaSegmentInfo?,
        subSegment: ShortDramaSegmentSubtitleInfo?
    ) {
        if (short?.dramasId == dramaId) {
            currentSegment.set(segment)
            currentSegmentPosition.value = short?.shotSegmentList?.indexOf(segment)
            setPreparingState()
            subtitles.set(subSegment?.subtitleContent ?: "")
            currentSegmentAudioDuration.set(player?.currentMediaItemDuration()?.toInt() ?: 0)
        } else {
            resetTrackingStats()
        }
    }

    private fun Player.currentMediaItemDuration(): Long {
        // 1. 获取当前的 Timeline
        val timeline = currentTimeline
        // 2. 检查 Timeline 是否为空（如果没有 prepare 或没有添加 MediaItem，它是空的）
        if (!timeline.isEmpty) {
            // 3. 创建一个 Window 对象来接收数据
            val window = Timeline.Window()
            // 4. 获取指定索引的 MediaItem 的信息
            // 检查索引是否有效
            if (currentMediaItemIndex < timeline.windowCount) {
                // 将数据填充到 window 对象中
                timeline.getWindow(currentMediaItemIndex, window)

                // 5. 获取时长 (单位：毫秒)
                val durationMs = window.durationMs

                // 注意：如果是直播流或尚未加载完成，时长可能是 C.TIME_UNSET
                if (durationMs != C.TIME_UNSET) {
                    //println("第 $mediaItemIndex 个视频的时长是: $durationMs ms")
                    return durationMs
                } else {
                    //println("时长未知或正在加载")
                }
            }
        }
        return 0
    }

    private fun updatePausedState() {
        val playing = isPlaying.value ?: false
        val preparingToPlay =
            isPreparingToPlay.value ?: (player?.playbackState == Player.STATE_BUFFERING)
        val shouldPaused = !playing && !preparingToPlay
        if (isPaused.value != shouldPaused) {
            isPaused.value = shouldPaused
        }
    }

    fun togglePlayback() {
        val state = player?.playbackState
        when {
            isPlaying.value == true -> {
                // 正在播放 -> 暂停
                resetPlayStatus()
                player?.pause()
            }

            isPreparingToPlay.value == true -> {
                // 准备中或自动翻页中 -> 取消操作
                resetPlayStatus()
            }

            state == Player.STATE_ENDED || state == Player.STATE_IDLE -> {
                Logger.i(LOG_TAG, "Restarting playback from state: $state")
                // 1. 跳转到开头
                player?.seekTo(0, 0)
                player?.prepare()
                // 3. 开始播放
                player?.play()
            }

            else -> {
                // 其他状态 -> 恢复播放
                resumeOrRestartPlayback()
            }
        }
    }

    /**
     * 暂停播放
     * @param onlyIfCurrentContent 默认为 true。
     * true: 只有当播放器当前播放的内容 ID 与本 ViewModel 的 dramaId 一致时，才执行暂停。
     * (用于 onStop 等生命周期回调，防止划走页面时误暂停了新页面)
     * false: 强制暂停 (用于用户点击暂停按钮，此时肯定是想暂停)
     */
    fun pausePlayback(onlyIfCurrentContent: Boolean = true) {
        cancelPlaybackTasks()
        resetPlayStatus()

        if (onlyIfCurrentContent) {
            // 【关键修复】只有播放器还在播我的内容时，我才有资格暂停它
            // 防止快速滑动时，上一个页面(已不可见)的 onStop 杀死了当前页面(可见)的播放
            if (player?.currentShort()?.dramasId == dramaId) {
                player?.pause()
            }
        } else {
            // 用户主动点击暂停，无条件执行
            player?.pause()
        }
    }

    private var detailLoadingJob: Job? = null

    // 新增一个 Job 用于监听列表变化
    private var listMonitoringJob: Job? = null
    fun startPreparedDetails() {
        Logger.i(LOG_TAG, "unreleased.get() =:${unreleased.get()} $")
        if (unreleased.get() == true) {
            launchWithLoading {
                response(mRepo.getUnreleasedShortDramaDetail(dramaId)) {
                    this.data?.let { updateShortDrama(it) }
                }
            }
        } else {
            if (manager == null) {
                launchWithLoading {
                    response(mRepo.getShortDramaDetail(dramaId)) {
                        this.data?.let { updateShortDrama(it) }
                    }
                }
            } else {
                detailLoadingJob?.cancel()
                detailLoadingJob = launch {
                    manager?.preparedMediaDetails
                        //转换：只提取当前 dramaId 对应的数据
                        ?.map { it[dramaId] }
                        // 1. ✅ 先去重：
                        //    - 如果是从 Object A 变成 null (重置)，这里会通过（因为 A != null）。
                        //    - 如果是从 null 变成 Object A (加载成功)，这里也会通过。
                        //    - 如果是从 Object A 变成 Object A (无关更新)，这里拦截。
                        ?.distinctUntilChanged()
                        // 2. ✅ 后过滤：
                        //    - 如果上一步通过的是 null (重置阶段)，这里拦截，防止 UI 闪烁或崩溃。
                        //    - 如果上一步通过的是 Object A (数据阶段)，这里放行，更新 UI。
                        ?.filterNotNull()
                        ?.collect { detail ->
                            updateShortDrama(detail)
                        }
                }

                listMonitoringJob?.cancel()
                listMonitoringJob = launch {
                    manager?.allLoadedMedias?.collect { list ->
                        // 如果当前 dramaId 存在于新的列表中，但 UI 可能还没刷新
                        // 我们调用 loadDramaDetail()。
                        // 放心：manager.ensurePlayableMedia 内部有 checks，如果已经加载过，不会重复发起网络请求，开销极小。
                        if (list.any { it.dramasId == dramaId }) {
                            loadDramaDetail(false)
                        }
                    }
                }
            }
        }
    }

    fun loadDramaDetail(force: Boolean = false) {
        manager?.ensurePlayableMedia(dramaId, force)
    }

    private fun updateShortDrama(dramaDetail: ShortDramaInfo) {
        seriesId = dramaDetail.dramasSeries?.dramasSeriesId
        currentShortDramaInfo = dramaDetail
        this.dramaDetail.value = dramaDetail
        this.dramasSeries.set(dramaDetail.dramasSeries)
        totalWorkNum.set(dramasSeries.get()?.totalWorkNum ?: 0)
        isMyShorts.set(User.get().isSameId(dramaDetail.artist?.userToken))
        autoUnlock.value = (dramaDetail.dramasSeries?.autoUnlockStatus == 1)
        autoUnlockSwitch.set(autoUnlock.value ?: false)
        val locked = dramaDetail.unlockInfo?.isLocked() == true && isMyShorts.get() != true
        unLockCoins.set(dramaDetail.unlockInfo?.unlockPrice ?: 0)
        if (autoUnlock.value == true && locked) {
            if (User.get().scoreBalance > (unLockCoins.get() ?: 0)) {
                unlock(false)
            } else {
                isLocked.value = true
            }
        } else {
            isLocked.value = locked
        }
        shortName.set(dramaDetail.dramasName)
        shortCover.set(dramaDetail.dramasCoverUrl)
        shortIntro.set(dramaDetail.scriptIntro)
        shortStyle.set(dramaDetail.styleName)
        dramaDetail.shotSegmentList?.let { segments ->
            shortDramaSegments.value = segments
        }
        Logger.i(LOG_TAG, "Update short drama,interaction=:${dramaDetail.interaction} $")
        dramaDetail.interaction?.let {
            likeNumStr.set(it.likeNumStr)
            followNumStr.set(it.watchingNumStr)
        }
        isLiked.set(dramaDetail.interaction?.likeStatus == 1)
        isFollowed.set(dramaDetail.interaction?.watchingStatus == 1)
    }

    private fun getCurrentSegment(): ShortDramaSegmentInfo? {
        return shortDramaSegments.value?.getOrNull(currentSegmentPosition.value ?: 0)
    }

    private fun resumeOrRestartPlayback() {
        if (player?.isPlaying == false) {
            player?.play()
        }
    }

    private fun setPreparingState() {
        if (player?.mediaItemCount != 0) {
            isPreparingToPlay.value = true
            resetPreparingStateTask = launchPlaybackTask {
                delay(PLAYER_PREPARING_DELAY_MS)
                isPreparingToPlay.value = false
            }
        }
    }

    private fun resetPlayStatus() {
        isPreparingToPlay.value = false
    }

    private fun cancelPlaybackTasks() {
        playbackTasks.forEach { it.cancel() }
        playbackTasks.clear()
    }


    private fun launchPlaybackTask(block: suspend () -> Unit): Job {
        return launch { block() }.also { playbackTasks.add(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        detailLoadingJob?.cancel()
        listMonitoringJob?.cancel()
        cancelPlaybackTasks()
        positionMonitoringJob?.cancel() // 记得取消轮询任务
    }

    override fun onResume() {
        super.onResume()
        //解决下一个预加载的item 数据不对的问题
        refreshInteractionStatus()
    }

    fun refreshInteractionStatus() {
        dramaDetail.value?.let { info ->
            val isFollow = info.interaction?.watchingStatus == 1
            if (isFollowed.get() != isFollow) {
                isFollowed.set(isFollow)
            }
            info.interaction?.watchingNumStr?.let {
                if (followNumStr.get() != it) followNumStr.set(it)
            }
        }
    }

    fun updateShotInfo(event: Events.SeriesWatchingEvent) {
        if (dramasSeries.get()?.dramasSeriesId == event.seriesId) {
            isFollowed.set(event.watchingStatus == 1)
            followNumStr.set(event.watchingNumStr)
            dramaDetail.value?.let {
                it.interaction?.watchingStatus = event.watchingStatus
                it.interaction?.watchingNumStr = event.watchingNumStr
                manager?.updateAllDataSourcesWithDetail(it)
            }
        }
    }

    fun updateShotInfo(event: Events.ShortDramasEditInfoEvent) {
        if (dramaId == event.dramaId) {
            shortName.set(event.dramaName)
            shortIntro.set(event.dramaIntro)
            dramaDetail.value?.let {
                it.dramasName = event.dramaName
                it.scriptIntro = event.dramaIntro
                manager?.updateAllDataSourcesWithDetail(it)
            }
        }
    }

    fun deleteShot() {
        launchWithLoading {
            response(mRepo.deleteShortDrama(dramaId)) {
                manager?.removeMediaById(dramaId)
                "删除成功".showToast()
                finish()
            }
        }
    }

    fun unlock(withLoading: Boolean = false) {
        launchWithLoading(withLoading, block = {
            val response = mRepo.unlockShortDrama(
                dramaId,
                dramaDetail.value?.dramasSeries?.dramasSeriesId
            )
            if (response.isSuccess) {
                dramaDetail.value?.let {
                    it.unlockInfo?.unlockStatus = 1
                    manager?.updateAllDataSourcesWithDetail(it)
                }
                isLocked.value = false
                if (withLoading) {
                    "解锁成功".showToast()
                } else {
                    "已为你自动解锁".showToast()
                }
                User.get().storeScoreBalance(response.data.pointBalance)
                FlowEventBus.post(Events.UnLockEvent(dramaId))
            } else {
                if (response.errorCode == ApiException.INSUFFICIENT_BALANCE) {
                    PointsRechargeActivity.start(topActivity, true)
                } else {
                    response.errorMsg?.showToast()
                }
                isLocked.value = true
            }
        }, onError = { isLocked.value = true })
        launch {
            val response =
                mRepo.autoUnlockSeries(dramasSeries.get()?.dramasSeriesId, autoUnlockSwitch.get())
            if (response.isSuccess) {
                autoUnlock.value = autoUnlockSwitch.get()
                FlowEventBus.post(Events.AutoUnLockSwitchEvent(dramasSeries.get()?.dramasSeriesId, autoUnlockSwitch.get()))
            }
        }
    }

    fun toggleFollow() {
        launch {
            response(
                mRepo.seriesFollow(
                    dramasSeries.get()?.dramasSeriesId,
                    if (isFollowed.get() == true) 0 else 1
                )
            ) {
                isFollowed.set(data.watchingStatus == 1)
                followNumStr.set(data.watchingNumStr)
                FlowEventBus.postSync(Events.SeriesWatchingEvent(dramasSeries.get()?.dramasSeriesId, data.watchingStatus ?: 0, followNumStr.get()))
            }
        }
    }

    fun toggleLike() {
        launch {
            response(mRepo.likeShortDrama(dramaId)) {
                isLiked.set(data.likeStatus == 1)
                likeNumStr.set(data.likeNumStr)
                FlowEventBus.postSync(Events.ShortDramaLikeEvent(dramaId, data.likeStatus ?: 0))
            }
        }
    }

    fun setAutoUnlock(auto: Boolean) {
        autoUnlockSwitch.set(auto)
    }

    fun publish(callback: (dramasSeriesId: String) -> Unit) {
        launchWithLoading {
            response(mRepo.publishShortDrama(dramaId)) {
                data?.dramasSeriesId?.let {
                    callback(it)
                    FlowEventBus.postSync(Events.SeriesUpdatedEvent(it))
                }
            }
        }
    }


}