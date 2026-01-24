/*
 * Copyright (C) 2009 Teleca Poland Sp. z o.o. <android@teleca.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kgmedia.fastfilm.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.feature.main.MainActivity
import com.kgmedia.fastfilm.kit.PlaylistListener
import com.kgmedia.fastfilm.kit.globalShortsPlayer
import com.kuaige.imageloader.GlideApp
import com.nhsd.basic.ext.dp
import com.nhsd.basic.utils.Logger

class PlayerService : Service(), PlaylistListener {
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var playAction: NotificationCompat.Action
    private lateinit var pauseAction: NotificationCompat.Action
    private lateinit var nextAction: NotificationCompat.Action
    private lateinit var preAction: NotificationCompat.Action
    private lateinit var likeAction: NotificationCompat.Action
    private lateinit var unLikeAction: NotificationCompat.Action
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playbackStateBuilder: PlaybackStateCompat.Builder

    companion object {
        const val TAG = "TrackPlayerService"
        const val CHANNEL_ID = "playerChannel"
        const val CHANNEL_NAME = "Player Service Channel"
        const val NOTIFICATION_ID = 10000

        @Volatile
        private var started = false
        private val lock = Any()

        fun isStarted() = started

        fun setStarted(value: Boolean) {
            synchronized(lock) {
                started = value
            }
        }

    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        initNotification(mediaSession)
        globalShortsPlayer.addListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        if (globalShortsPlayer.isPlaying) {
            globalShortsPlayer.currentShort()?.let {
                updateNotificationWithNewTrack(true, it)
            }
        }
        startForegroundService()
//        requestAudioFocus()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.i("__player onStartCommand")
        // 优化：确保只在服务未启动时调用 startForeground
        if (!isStarted()) {
            startForegroundService()
        }
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_STICKY

    }

    private fun startForegroundService() {
//        startForeground(
//            NOTIFICATION_ID,
//            getNotification(
//                globalShortsPlayer.isPlaying,
//                globalShortsPlayer.currentShort()?.isLike()
//            )
//        )
        setStarted(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setCallback(object : MediaSessionCompat.Callback() {

                override fun onPlay() {
                    super.onPlay()
                    globalShortsPlayer.play()
                    ManualStatus.update(ManualStatus.Status.PLAY)
                }

                override fun onPause() {
                    super.onPause()
                    ManualStatus.update(ManualStatus.Status.PAUSE)
                    globalShortsPlayer.pause()
                }

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    ManualStatus.update(ManualStatus.Status.PREVIOUS_OR_NEXT)
                    globalShortsPlayer.next()
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    ManualStatus.update(ManualStatus.Status.PREVIOUS_OR_NEXT)
                    globalShortsPlayer.prev()
                }

                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    ManualStatus.update(ManualStatus.Status.SEEK_TO)
                    globalShortsPlayer.seekTo(pos)
                }

                override fun onRewind() {
                    Logger.i("on reward")
                    if (User.get().isVisitor) {
                        return
                    }
                    like(true)
                }

                override fun onFastForward() {
                    super.onFastForward()
                    if (User.get().isVisitor) {
                        return
                    }
                    like(false)
                }
            })
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

            playbackStateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_REWIND or PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            setPlaybackState(playbackStateBuilder.build())
            setActive(true)
        }
        setMediaData(short = globalShortsPlayer.currentShort())
    }

    // 更新通知和前台服务状态
    private fun updateNotificationAndForegroundService(isPlaying: Boolean, isLike: Boolean?) {
        playbackStateBuilder.setState(
            if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
            globalShortsPlayer.playbackPosition(),
            1.0f
        )
        mediaSession.setPlaybackState(playbackStateBuilder.build())
        notificationManager.notify(
            NOTIFICATION_ID,
            getNotification(isPlaying, isLike)
        )
    }

    // 初始化通知管理器和通知构建器
    private fun initNotification(mediaSession: MediaSessionCompat) {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 创建播放和暂停的动作
        playAction = NotificationCompat.Action(
            R.drawable.ic_play,
            "Play",
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
        )
        pauseAction = NotificationCompat.Action(
            R.drawable.ic_pause,
            "Pause",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PAUSE
            )
        )
        nextAction = NotificationCompat.Action(
            R.drawable.ic_next,
            "Next",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
        )
        preAction = NotificationCompat.Action(
            R.drawable.ic_previous,
            "Previous",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        )
        likeAction = NotificationCompat.Action(
            R.drawable.ic_notice_like,
            "Like",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_REWIND
            )
        )
        unLikeAction = NotificationCompat.Action(
            R.drawable.ic_liked,
            "unLike",
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_FAST_FORWARD
            )
        )
        // 初始化通知构建器，不添加任何动作，将在 getNotification 方法中根据状态添加
        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kuaige_linceer)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(null)
            .setVibrate(null)
            .setOngoing(false)//.setOnlyAlertOnce(true)
        addActions()
    }

    private fun addActions() {
        if (User.get().isLogged) {
            notificationBuilder.addAction(likeAction)
        }
        notificationBuilder.addAction(preAction)
        notificationBuilder.addAction(playAction)
        notificationBuilder.addAction(nextAction)
    }


    private fun getNotification(isPlaying: Boolean?, isLike: Boolean?): Notification {
        // 更新播放/暂停按钮
        updateNotificationActions(isPlaying, isLike)

        return notificationBuilder.build()
    }

    @SuppressLint("RestrictedApi")
    private fun updateNotificationActions(isPlaying: Boolean?, isLike: Boolean?): Boolean {
        var change = false
        updateWithLoginStatus()
        var index = 0
        if (User.get().isLogged) {
            if (isLike == true) {
                if (notificationBuilder.mActions[index] != unLikeAction) {
                    notificationBuilder.mActions[index] = unLikeAction
                    change = true
                }
            } else {
                if (notificationBuilder.mActions[index] != likeAction) {
                    notificationBuilder.mActions[index] = likeAction
                    change = true
                }
            }
            index++
        }
        if (notificationBuilder.mActions[index] != preAction) {
            notificationBuilder.mActions[index] = preAction
            change = true
        }
        index++
        // 更新播放/暂停动作
        isPlaying?.let {
            if (isPlaying) {
                if (notificationBuilder.mActions[index] != pauseAction) {
                    notificationBuilder.mActions[index] = pauseAction
                    change = true
                }
            } else {
                if (notificationBuilder.mActions[index] != playAction) {
                    notificationBuilder.mActions[index] = playAction
                    change = true
                }
            }
        }
        index++
        if (notificationBuilder.mActions[index] != nextAction) {
            notificationBuilder.mActions[index] = nextAction
            change = true
        }
        return change
    }

    private fun updateWithLoginStatus() {
        if (User.get().isVisitor) {
            if (notificationBuilder.mActions.size != 3) {
                notificationBuilder.clearActions()
                addActions()
            }
        } else {
            if (notificationBuilder.mActions.size != 4) {
                notificationBuilder.clearActions()
                addActions()
            }
        }
    }

    override fun onShortChanged(short: ShortDramaInfo?) {
        super.onShortChanged(short)
        short?.let {
            setMediaData(short)
            updateNotificationWithNewTrack(null, short)
        }
    }

    override fun onShortProgress(milliseconds: Long) {
        super.onShortProgress(milliseconds)
//        updateNotificationAndForegroundService(globalTrackListPlayer.isPlaying, globalTrackListPlayer.currentTrack()?.isLike())
        playbackStateBuilder.setState(
            PlaybackStateCompat.STATE_PLAYING,
            globalShortsPlayer.playbackPosition(),
            1.0f
        )
        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }

    // 更新通知以显示新的歌曲信息
    private fun updateNotificationWithNewTrack(isPlaying: Boolean?, track: ShortDramaInfo?) {
        // 更新通知内容
        notificationBuilder
            .setContentTitle(track?.dramasName)
            .setContentText(track?.artist?.nickName)
        setLargeIcon(track)
//        updateNotificationActions(isPlaying, track?.isLike())
        // 更新通知
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun setLargeIcon(track: ShortDramaInfo?) {
        GlideApp.with(this).asBitmap().load(track?.dramasCoverUrl)
            .into(object : CustomTarget<Bitmap>(50.dp, 50.dp) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    notificationBuilder.setLargeIcon(resource)
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Handle placeholder or clear the large icon if necessary
                }
            })
    }

    override fun onPlayingStatusChange(status: PlaylistListener.Status?) {
        when (status) {
            PlaylistListener.Status.STARTED -> {
                Logger.i(App.TAG, "__player state STARTED....")
                playbackStateBuilder.setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    globalShortsPlayer.playbackPosition(),
                    1.0f
                )
                mediaSession.setPlaybackState(playbackStateBuilder.build())

//                notificationManager.notify(
//                    NOTIFICATION_ID,
//                    getNotification(true, globalShortsPlayer.currentShort()?.isLike())
//                )
            }

            PlaylistListener.Status.PAUSED -> {
//                if (!ManualStatus.isResume()) {
                Logger.i(
                    App.TAG,
                    "__player state PAUSED.... is resume status=${ManualStatus.isResume()}"
                )

                playbackStateBuilder.setState(
                    PlaybackStateCompat.STATE_PAUSED,
                    globalShortsPlayer.playbackPosition(),
                    1.0f
                )
                mediaSession.setPlaybackState(playbackStateBuilder.build())
                if (!ManualStatus.isResume()) {
//                    notificationManager.notify(
//                        NOTIFICATION_ID,
//                        getNotification(false, globalShortsPlayer.currentShort()?.isLike())
//                    )
                }
//                }
                ManualStatus.resetStatus()
            }

            PlaylistListener.Status.STOPPED -> {
                Logger.i("__player is stop......")
                notificationManager.cancel(NOTIFICATION_ID)
                stopForeground(true)
                stopSelf()
                started = false
            }

            else -> {
                // Handle other statuses if necessary
            }
        }
    }

    private fun setMediaData(short: ShortDramaInfo?) {
        val metaData = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, short?.dramasName)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, short?.artist?.nickName)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, short?.getDurationMs() ?: 0)

        mediaSession.setMetadata(metaData.build())
    }

    fun like(liked: Boolean) {
        globalShortsPlayer.currentShort()?.let {
            val newStatus = if (liked) 1 else 0
//            val likedNum = if (liked) it.interaction?.likeNum?.plus(1)
//                ?: 1 else it.interaction?.likeNum?.minus(1) ?: 0
//            it.interaction?.likeStatus = newStatus
//            it.interaction?.likeNum = likedNum
//            ioScope.launch {
//                val response = ApiFactory.apiService.likeTrack(LikeTrackBody(it.trackId, newStatus))
//                if (response.isSuccess) {
//                    EventBus.getDefault()
//                        .post(Events.TrackLikeEvent(it.trackId, newStatus, likedNum))
//                }
//            }
            updateNotificationAndForegroundService(globalShortsPlayer.isPlaying, liked)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        setStarted(false)
        mediaSession.apply {
            setActive(false)
            release()
        }
        stopForeground(true)  // 先停止前台服务
        notificationManager.cancel(NOTIFICATION_ID)
    }

    object ManualStatus {
        enum class Status {
            SEEK_TO, PREVIOUS_OR_NEXT, PAUSE, PLAY, STOP
        }

        var next: Status? = null

        fun isResume(): Boolean {
            return next == Status.SEEK_TO || next == Status.PREVIOUS_OR_NEXT
        }

        fun resetStatus() {
            next = null
        }

        fun update(status: Status) {
            Logger.i("__player update status :$status")
            next = status
        }
    }


}
