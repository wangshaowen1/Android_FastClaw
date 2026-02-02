package com.kgmedia.fastfilm.feature.timbre

import android.os.Handler
import android.os.Looper
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.SceneCostBody
import com.kgmedia.fastfilm.core.network.apis.response.SceneCostData
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.kit.media.PlayerFactory

/**
 * 新建克隆音色弹窗
 */
class CreateCloneTimbreViewModel : AppRepositoryViewModel<AppRepository>(), Player.Listener {
    var costData = ObservableField<SceneCostData>()
    val playing by lazy { ObservableBoolean(false) }
    val playTime by lazy { ObservableField<String>() }
    val audioPath by lazy { mBundle.getString(CreateCloneTimbreDialog.AUDIO_PATH) }
    val audioDuring by lazy { mBundle.getLong(CreateCloneTimbreDialog.AUDIO_DURING) }
    var countDownTime: Long? = null
    val hander by lazy { Handler(Looper.getMainLooper()) }
    val player by lazy {
        PlayerFactory.createPlayer(false).apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    playing.set(isPlaying)
                    if (isPlaying) {
                        countDownTime = audioDuring
                        hander.postDelayed(runnable, 100)
                    } else {
                        playTime.set(DateUtil.secToTimeMis(audioDuring))
                        hander.removeCallbacks(runnable)
                    }
                }
            })
        }
    }

    val runnable: Runnable by lazy {
        Runnable {
            countDownTime = (countDownTime ?: 0) - 100
            if (countDownTime!! >= 0) {
                playTime.set(DateUtil.secToTimeMis(countDownTime!!))
                hander.postDelayed(runnable, 100)
            } else {
                playTime.set(DateUtil.secToTimeMis(audioDuring))
                hander.removeCallbacks(runnable)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        playTime.set(DateUtil.secToTimeMis(audioDuring))
    }

    fun getCost() {
        launchWithLoading {
            response(mRepo.api.sceneCost(SceneCostBody(SceneCostBody.SCENE_TYPE_VOICE_CLONE))) {
//                User.get().storeScoreBalance(data.remainingPoints)
                costData.set(data)
            }
        }
    }

    fun playRecordAudio() {
        if (playing.get()) {
            player.stop()
            return
        }
        audioPath?.let {
            player.setMediaItem(MediaItem.Builder().setUri(it).build())
            player.playWhenReady = true
            player.prepare()
            playing.set(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        hander.removeCallbacks(runnable)
        player.release()
    }
}