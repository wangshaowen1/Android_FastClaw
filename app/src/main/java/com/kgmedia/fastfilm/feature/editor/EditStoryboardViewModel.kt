package com.kgmedia.fastfilm.feature.editor

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.Player
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.kit.media.PlayerFactory
import com.kgmedia.fastfilm.kit.media.play
import com.nhsd.basic.ext.getParcelableValue

/**
 * 编辑分镜视频
 */
class EditStoryboardViewModel : AppRepositoryViewModel<EditStoryboardRepository>() {
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val storyId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val storyboard by lazy { ObservableField<SimpleStoryboardInfo>() }
    val workRatio = "9:16"

    val storyboardPlayer by lazy {
        PlayerFactory.createPlayer(true, autoAudioFocus = true).apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_READY -> {
                        }

                        Player.STATE_ENDED -> {
                            seekTo(0)
                            play()
                            playing.set(true)
                        }
                    }
                }
            })
        }
    }
    val playing by lazy { ObservableBoolean(true) }

    val storyboardDesc by lazy { MutableLiveData<String>() }
    /**
     * 图片生成状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]
     */
    val imageGenStatus by lazy { ObservableInt() }
    /**
     * 视频生成状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]
     */
    val videoGenStatus by lazy { ObservableInt() }

    override fun onCreate() {
        super.onCreate()
        val info = mBundle.getParcelableValue(Constants.Extra.DATA, SimpleStoryboardInfo::class.java)
        if (info == null) {
            finish()
            return
        }
        storyboardDesc.value = info.storyBoardDesc
        //任务类型 [1=文生文 2=文生图 3=图生图 4=文生视频 5图生视频 6-视频生视频]
        when (info.taskType) {
            1, 2, 3 -> {
                imageGenStatus.set(info.stepStatus ?: -1)
                videoGenStatus.set(-1)
            }
            4, 5, 6 ->{
                imageGenStatus.set(2)
                videoGenStatus.set(info.stepStatus ?: -1)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        storyboardPlayer.release()
    }

    fun play() {
        if (storyboardPlayer.isPlaying) {
            storyboardPlayer.pause()
            playing.set(false)
        } else {
            when (storyboardPlayer.playbackState) {
                Player.STATE_IDLE -> {
                    val videoUrl = storyboard.get()?.videoUrl
                    if (videoUrl.isNullOrEmpty().not()) {
                        storyboardPlayer.play(videoUrl)
                        playing.set(true)
                    }
                }

                Player.STATE_ENDED -> {
                    storyboardPlayer.seekTo(0)
                    storyboardPlayer.play()
                    playing.set(true)
                }

                else -> {
                    storyboardPlayer.play()
                    playing.set(true)
                }
            }
        }
    }

    /**
     * [枚举]创作流程状态 [0=草稿 1=生成中 2=已完成 3=生成失败 4=已合成]
     */
    fun videoGenStatusText(videoGenStatus: Int?) =
        when (videoGenStatus) {
            1 -> "视频生成中"
            3 -> "视频生成失败"
            else -> null
        }

    /**
     * 视频生成中
     */
    fun videoGenerating(videoGenStatus: Int?) = videoGenStatus == 1

    /**
     * 视频已生成
     */
    fun videoGenerated(videoGenStatus: Int?) = videoGenStatus == 2 || videoGenStatus == 4

    /**
     * 图片生成中
     */
    fun imageGenerating(imageGenStatus: Int?) = imageGenStatus == 1

    /**
     * 图片已生成
     */
    fun imageGenerated(imageGenStatus: Int?) = imageGenStatus == 2 || imageGenStatus == 4

    fun retryGenerate(){

    }
}