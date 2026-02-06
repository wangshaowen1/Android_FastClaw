package com.kgmedia.fastfilm.feature.editor

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.Player
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.kit.media.PlayerFactory
import com.kgmedia.fastfilm.kit.media.play
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 编辑分镜视频
 */
class EditStoryboardViewModel : AppRepositoryViewModel<EditStoryboardRepository>() {
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

    private var loopImageJob: Job? = null
    private var loopVideoJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val info = mBundle.getParcelableValue(Constants.Extra.DATA, SimpleStoryboardInfo::class.java)
        if (info == null) {
            finish()
            return
        }
        storyboard.set(info)
        storyboardDesc.value = info.storyBoardDesc
        if (info.videoGenerating) {
            loopVideo(info.taskId)
        } else if (info.imageGenerating) {
            loopImage(info.taskId)
        }
        play()
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
     * @param genStatusVideo [枚举]视频生成状态 0=待生成 1=生成中 2=成功 3=失败
     */
    fun videoGenStatusText(genStatusVideo: Int?) =
        when (genStatusVideo) {
            1 -> "视频生成中"
            3 -> "视频生成失败"
            else -> null
        }

    /**
     * 视频失败重试
     */
    fun retryGenerateVideo(finish: Boolean = false) {
        val storyboard = storyboard.get() ?: return
        if (storyboard.imageGenerating) {
            "图片正在生成中，请稍后再试".showToast()
            return
        }
        if (storyboard.videoGenerating) {
            "视频正在生成中，请稍后再试".showToast()
            return
        }
        launchWithLoading {
            val body = GenerateStoryboardBody(
                storyboardId = storyboard.storyboardId,
                storyBoardDesc = storyboard.storyBoardDesc,
                genImagePrompt = storyboard.genImagePrompt,
                genVideoPrompt = storyboard.genVideoPrompt,
                workId = storyboard.workId,
                storyId = storyboard.storyId
            )
            response(ApiFactory.apiService.workStoryboardVideoSingleGenerate(body)) {
                if (finish) {
                    finish()
                    return@response
                }
                storyboard.taskId = data?.taskId
                storyboard.genStatusVideo = data?.genStatus
                this@EditStoryboardViewModel.storyboard.notifyChange()
                loopVideo(data?.taskId)
            }
        }
    }

    /**
     * 重新生成图片
     */
    fun reGenerateImage() {
        val storyboard = storyboard.get() ?: return
        if (storyboard.imageGenerating) {
            "图片正在生成中，请稍后再试".showToast()
            return
        }
        if (storyboard.videoGenerating) {
            "视频正在生成中，请稍后再试".showToast()
            return
        }
        val storyboardDesc = storyboardDesc.value
        if (storyboardDesc.isNullOrEmpty()) {
            "请先填写分镜描述".showToast()
            return
        }
        storyboard.storyBoardDesc = storyboardDesc
        launchWithLoading {
            val body = GenerateStoryboardBody(
                storyboardId = storyboard.storyboardId,
                storyBoardDesc = storyboardDesc,
                genImagePrompt = storyboard.genImagePrompt,
                genVideoPrompt = storyboard.genVideoPrompt,
                workId = storyboard.workId,
                storyId = storyboard.storyId
            )
            response(ApiFactory.apiService.workStoryboardSingleGenerate(body)) {
                storyboard.taskId = data?.taskId
                storyboard.genStatusImage = data?.genStatus
                this@EditStoryboardViewModel.storyboard.notifyChange()
                loopImage(data?.taskId)
            }
        }
    }

    /**
     * 重新生成视频
     */
    fun reGenerateVideo() {
        retryGenerateVideo(true)
    }

    private fun loopImage(taskId: String?) {
        loopImageJob?.cancel()
        loopImageJob = launch {
            loop@ while (storyboard.get()?.imageGenerating == true) {
                runCatching {
                    val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workStoryboardSingleStatus(TaskIdBody(taskId, 71)).data }
                    storyboard.get()?.apply {
                        genStatusImage = taskStatus?.genStatus
                        imageUrl = taskStatus?.result
                    }
                    storyboard.notifyChange()
                }
                if (storyboard.get()?.imageGenerating != true) {
                    break@loop
                }
                delay(3000)
            }
        }
    }

    private fun loopVideo(taskId: String?) {
        loopVideoJob?.cancel()
        loopVideoJob = launch {
            loop@ while (storyboard.get()?.videoGenerating == true) {
                runCatching {
                    val taskStatus = withContext(Dispatchers.IO) { ApiFactory.apiService.workStoryboardSingleStatus(TaskIdBody(taskId, 72)).data }
                    storyboard.get()?.apply {
                        genStatusVideo = taskStatus?.genStatus
                        videoUrl = taskStatus?.result
                    }
                    storyboard.notifyChange()
                }
                if (storyboard.get()?.videoGenerating != true) {
                    if (storyboard.get()?.videoGenerated == true) {
                        play()
                    }
                    break@loop
                }
                delay(3000)
            }
        }
    }

    fun save(finish: Boolean = false) {
        val storyboard = storyboard.get() ?: return
        val storyboardDesc = storyboardDesc.value
        if (storyboardDesc.isNullOrEmpty()) {
            "请先填写分镜描述".showToast()
            return
        }
        storyboard.storyBoardDesc = storyboardDesc
        launchWithLoading {
            val body = SaveStoryboardBody(
                storyboard.storyboardId,
                storyboardDesc,
                storyboard.genImagePrompt,
                storyboard.genVideoPrompt,
                storyboard.imageUrl,
                storyboard.videoUrl,
                storyboard.duration,
                storyboard.workId,
                storyboard.storyId
            )
            response(ApiFactory.apiService.workStoryboardSave(body)) {
                "保存成功".showToast()
                if (finish) {
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        loopImageJob?.cancel()
        loopVideoJob?.cancel()
        super.onDestroy()
    }

}