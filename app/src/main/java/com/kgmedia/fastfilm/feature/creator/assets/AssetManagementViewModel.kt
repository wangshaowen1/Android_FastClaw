package com.kgmedia.fastfilm.feature.creator.assets

import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository

/**
 * 资产管理
 */
class AssetManagementViewModel : AppRepositoryViewModel<AppRepository>() {
    val loadState by lazy { ObservableInt() }

    fun getAssets() {

//        if (storyboard.get() == null || music.get() == null) loadState.set(LoadStateViewHolder.LOADING)
//        launch(
//            block = {
//                val info = withContext(Dispatchers.IO) { mRepo.mvStoryboardInfo(mvId).data }
//                if (info == null) {
//                    updateLoadState()
//                    return@launch
//                }
//                if (music.get() == null) {
//                    val music = withContext(Dispatchers.IO) { mRepo.api.getTrackDetails(TrackIdBody(info.musicId)).data }
//                    if (music == null) {
//                        updateLoadState()
//                        return@launch
//                    }
//                    this@EditorViewModel.music.set(music)
//                    music.duration?.let { duration.set(it) }
//                    music.musicPlayUrl()?.let { audioPlayer.startPlay(it) }
//                }
//                info.calculatePlayPosition()
//                storyboard.set(info)
//                info.segments?.map {
//                    ItemStoryboardVM(it).apply {
//                        onItemClick.value = { v ->
//                            if (this != currentSegment.get()) {
//                                select()
//                                audioPlayer.seekTo(it.startPositionMs)
//                            }
//                        }
//                    }
//                }.let { adapter.submitList(it) }
//                nextStepEnabled.set(info.storyBoardMvAvailableStatus == 1)
//                loopGeneratingSegments()
//                updateLoadState()
//            },
//            onError = {
//                updateLoadState()
//            }
//        )
    }

    private fun updateLoadState() {
//        if (storyboard.get() == null || music.get() == null) {
//            loadState.set(LoadStateViewHolder.RETRY)
//        } else {
//            loadState.set(LoadStateViewHolder.CONTENT)
//        }
    }

}