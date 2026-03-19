package com.kgmedia.fastfilm.feature.editor

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.Player
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.sensitiveWordCheck
import com.kgmedia.fastfilm.core.network.apis.entity.SimpleStoryboardInfo
import com.kgmedia.fastfilm.core.network.apis.request.GenerateStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.SaveStoryboardBody
import com.kgmedia.fastfilm.core.network.apis.request.StrategyTypeBody
import com.kgmedia.fastfilm.core.network.apis.request.TaskIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.kgmedia.fastfilm.kit.media.PlayerFactory
import com.kgmedia.fastfilm.kit.media.play
import com.nhsd.basic.bus.FlowEventBus
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
    val workRatio by lazy { mBundle.getString(Constants.Extra.VIDEO_RATIO, "9:16") ?: "9:16" }

    /**
     * 剧集状态 [枚举]创作流程进度 [10=剧本输入 15=剧本生成中 20=资产生成中 30=分镜描述中 32=分镜视频生成中35待合成 40=已完成 45=分镜重绘中 50=重绘已完成]
     */
    val storyStatus by lazy { mBundle.getInt(Constants.Extra.STATUS, 0) }

    val isSynthesised by lazy { storyStatus in arrayOf(40, 45, 50) }

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

    val storyBoardDesc by lazy { MutableLiveData<CharSequence>() }
    val genVideoCost by lazy { ObservableInt(0) }
    private var genVideoPrice = 0
    private var loopVideoJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val info = mBundle.getParcelableValue(Constants.Extra.DATA, SimpleStoryboardInfo::class.java)
        if (info == null) {
            finish()
            return
        }
        getGenPrice()
        storyboard.set(info)
        storyBoardDesc.value = info.storyBoardDesc
        if (info.videoGenerating) {
            loopVideo(info.taskId)
        } else {
            play()
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
        if (storyboard.videoGenerating) {
            "视频正在生成中，请稍后再试".showToast()
            return
        }
        val prompt = storyBoardDesc.value?.toString()
        if (prompt.isNullOrEmpty()) {
            "请先填写分镜描述".showToast()
            return
        }
        val cost = genVideoCost.get()
        if (cost == 0 && genVideoPrice == 0) {
            getGenPrice()
            return
        }
        generate(storyboard, prompt, cost, finish)
    }

    private fun generate(storyboard: SimpleStoryboardInfo, prompt: String, cost: Int, finish: Boolean = false) {
        if (PowerRechargeDialog.checkLowBalance(cost)) return
        launchWithLoading {
            if (prompt != storyboard.storyBoardDesc) {
                sensitiveWordCheck(prompt).let { (isPass, highlightText) ->
                    if (!isPass) {
                        highlightText?.let { storyBoardDesc.value = it }
                        return@launchWithLoading
                    }
                }
                val response = withContext(Dispatchers.IO) {
                    val body = SaveStoryboardBody(
                        workId = storyboard.workId,
                        storyId = storyboard.storyId,
                        storyboardId = storyboard.storyboardId,
                        storyBoardDesc = prompt
                    )
                    mRepo.api.workStoryboardSave(body)
                }
                val data = response.data
                if (response.isSuccess && data != null) {
                    this@EditStoryboardViewModel.storyboard.set(data)
                    FlowEventBus.postSync(Events.StoryboardSavedEvent(data))
                } else {
                    response.msg?.showToast()
                    return@launchWithLoading
                }
            }
            val body = GenerateStoryboardBody(
                storyboardId = storyboard.storyboardId,
                workId = storyboard.workId,
                storyId = storyboard.storyId,
                operateType = if (isSynthesised) 3 else 2
            )
            response(mRepo.api.workStoryboardVideoSingleGenerate(body)) {
                FlowEventBus.postSync(Events.StoryboardVideoRegenerateEvent(storyboard.workId, storyboard.storyId, storyboard.storyboardId))
                if (finish && data?.genStatus.let { it == 1 || it == 2 }) {
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
     * 重新生成视频
     */
    fun reGenerateVideo() {
        retryGenerateVideo(true)
    }

    private fun loopVideo(taskId: String?) {
        loopVideoJob?.cancel()
        loopVideoJob = launch {
            loop@ while (storyboard.get()?.videoGenerating == true) {
                runCatching {
                    val taskStatus = withContext(Dispatchers.IO) { mRepo.api.workStoryboardSingleStatus(TaskIdBody(taskId, 72)).data }
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
        val prompt = storyBoardDesc.value?.toString()
        if (prompt.isNullOrEmpty()) {
            "请先填写分镜描述".showToast()
            return
        }
        launchWithLoading {
            sensitiveWordCheck(prompt).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { storyBoardDesc.value = it }
                    return@launchWithLoading
                }
            }
            val body = SaveStoryboardBody(
                workId = storyboard.workId,
                storyId = storyboard.storyId,
                storyboardId = storyboard.storyboardId,
                storyBoardDesc = prompt
            )
            response(mRepo.api.workStoryboardSave(body)) {
                data?.let {
                    this@EditStoryboardViewModel.storyboard.set(it)
                    FlowEventBus.postSync(Events.StoryboardSavedEvent(it))
                }
                "保存成功".showToast()
                if (finish) {
                    finish()
                }
            }
        }
    }

    private fun getGenPrice() {
        launch {
            withContext(Dispatchers.IO) {
                mRepo.api.queryAiPrice(StrategyTypeBody(3)).data?.pointNum?.let {
                    genVideoPrice = it
                    genVideoCost.set(it * (storyboard.get()?.duration ?: 0))
                }
            }
        }
    }

    override fun onDestroy() {
        loopVideoJob?.cancel()
        super.onDestroy()
    }

}