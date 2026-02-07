package com.kgmedia.fastfilm.feature.editor

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.Player
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.RetrofitClient
import com.kgmedia.fastfilm.core.network.apis.request.IdsBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.kit.media.PlayerFactory
import com.kgmedia.fastfilm.kit.media.play
import com.kgmedia.fastfilm.kit.media.setPlayUrl
import com.kgmedia.fastfilm.widget.GeneratingDialog
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.kuaige.videoedit.resource.UrlResource
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 编辑分镜视频
 */
class EditStoryboardVideoViewModel : AppRepositoryViewModel<EditStoryboardRepository>() {
    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val storyTitle by lazy { mBundle.getString(Constants.Extra.TITLE) }
    val storyId by lazy { mBundle.getString(Constants.Extra.STORY_ID) }
    val taskId by lazy { mBundle.getString(Constants.Extra.TASK_ID) }
    val workRatio by lazy { mBundle.getString(Constants.Extra.VIDEO_RATIO, "9:16") ?: "9:16" }
    val loadState by lazy { ObservableInt() }
    val currentStoryboard by lazy { ObservableField<ItemStoryboardVM>() }
    val scrollToItem by lazy { MutableLiveData<Int>(-1) }
    val adapter by lazy { QuickAdapter<ItemStoryboardVM>(R.layout.item_short_drama_editor_segment, emptyList()) }
    val nextStepEnabled by lazy { ObservableBoolean(false) }
    val storyboardPlayer by lazy {
        PlayerFactory.createPlayer(true, autoAudioFocus = true).apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    when (playbackState) {
                        Player.STATE_READY -> {
                        }

                        Player.STATE_ENDED -> {
                            playNext()
                        }
                    }
                }
            })
        }
    }
    val playing by lazy { ObservableBoolean(true) }
    private var synthesisDialog: GeneratingDialog? = null
    private var synthesisTask: VideoSynthesisTask? = null
    private var loopSynthesisStatusJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        getStoryboardList()
    }

    override fun onCleared() {
        loopSynthesisStatusJob?.cancel()
        adapter.items.forEach { it.clear() }
        super.onCleared()
        storyboardPlayer.release()
    }

    private fun loopSynthesisStatus() {
        loopSynthesisStatusJob?.cancel()
        loopSynthesisStatusJob = launch {
            val task = synthesisTask
            if (task != null) {
                if (task.status == VideoSynthesisTask.STATUS_SUCCESS) {
                    synthesisDialog?.dismiss()
                    finish()
                } else if (task.isSynthesising()) {
                    delay(1000)
                    loopSynthesisStatus()
                } else if (task.isFailed()) {
                    synthesisDialog?.dismiss()
                    task.reason?.showToast()
                }
            }
        }
    }

    fun getStoryboardList() {
        if (adapter.items.isEmpty()) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                val list = withContext(Dispatchers.IO) { mRepo.api.workStoryboardList(WorkBody(workId, storyId)).data }
                if (list.isNullOrEmpty()) {
                    updateLoadState()
                    return@launch
                }
                list.mapIndexed { index, info ->
                    ItemStoryboardVM(workId, storyId, info).apply {
                        onItemClick.value = { v ->
                            if (this != currentStoryboard.get()) {
                                select()
                            }
                        }
                    }
                }.let { adapter.submitList(it) }
                nextStepEnabled.set(allStoryboardGenerated())
                if (currentStoryboard.get() == null) {
                    adapter.items.firstOrNull()?.select()
                }
                updateLoadState()
            },
            onError = {
                updateLoadState()
            }
        )
    }

    private fun updateLoadState() {
        if (adapter.items.isEmpty()) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    fun play() {
        if (storyboardPlayer.isPlaying) {
            storyboardPlayer.pause()
            playing.set(false)
        } else {
            when (storyboardPlayer.playbackState) {
                Player.STATE_IDLE -> {
                    val videoUrl = currentStoryboard.get()?.data?.get()?.videoUrl
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

    fun stopPlay() {
        storyboardPlayer.stop()
        playing.set(false)
    }

    private fun playNext() {
        val current = currentStoryboard.get()
        val currentIndex = adapter.items.indexOf(current)
        val next = if (currentIndex > -1 && currentIndex < adapter.items.size - 1) adapter.getItem(currentIndex + 1) else adapter.getItem(0)
        next.select()
    }

    private fun ItemStoryboardVM?.select() {
        currentStoryboard.get()?.selected?.set(false)
        this?.selected?.set(true)
        currentStoryboard.set(this)
        adapter.items.indexOf(this).takeIf { it > -1 }?.let { scrollToItem.value = it }
        val videoUrl = currentStoryboard.get()?.data?.get()?.videoUrl
        if (videoUrl.isNullOrEmpty().not()) {
            if (playing.get()) {
                storyboardPlayer.play(videoUrl)
            } else {
                storyboardPlayer.playWhenReady = false
                storyboardPlayer.setPlayUrl(videoUrl)
                storyboardPlayer.prepare()
            }
        } else {
            storyboardPlayer.clearMediaItems()
            storyboardPlayer.pause()
            playing.set(false)
        }
    }

    private fun allStoryboardGenerated(): Boolean {
        return adapter.items.all { it.data.get()?.videoGenerated == true }
    }

    fun anyStoryboardGenerating(): Boolean {
        return adapter.items.any { it.data.get()?.videoGenerating == true }
    }

    fun anyStoryboardFailed(): Boolean {
        return adapter.items.any { it.data.get()?.videoGenerateFailed == true }
    }

    fun download(videoUrl: String) {
        launchWithLoading {
            val resource = UrlResource(videoUrl, RetrofitClient.httpClient)
            resource.download { }
            val destFile = File(PathUtils.getExternalDownloadsPath(), resource.file.name)
            val copied = if (destFile.exists()) true else withContext(Dispatchers.IO) { FileUtils.copy(resource.file, destFile) }
            if (copied) {
                FileUtils.notifySystemToScan(destFile)
                "视频已保存至相册".showToast()
            } else {
                "保存失败".showToast()
            }
        }
    }

    fun deleteCurrentStoryboard() {
        launchWithLoading {
            response(mRepo.api.workStoryboardDelete(IdsBody(null, currentStoryboard.get()?.data?.get()?.storyboardId))) {
                val currentStoryboard = currentStoryboard.get()
                playNext()
                currentStoryboard?.let { adapter.remove(it) }
            }
        }
    }

    fun synthesis() {
        if (anyStoryboardGenerating()) {
            "视频正在生成中，请稍后再试".showToast()
            return
        }
        if (anyStoryboardFailed()) {
            "视频生成失败，请重新生成".showToast()
            return
        }
        val storyboards = adapter.items.mapNotNull { it.data.get()?.videoUrl }
        if (storyboards.isEmpty()) {
            return
        }
        topActivity?.let {
            val dialog = GeneratingDialog(it, "视频合成中...")
            synthesisDialog = dialog
            dialog.show()
        }
        val task = VideoSynthesisTask(workId, storyId, storyboards)
        synthesisTask = task
        VideoSynthesisManager.synthesis(task)
        loopSynthesisStatus()
    }

}