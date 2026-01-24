package com.kgmedia.fastfilm.feature.editor.history

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorRepository
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast

/**
 * 分镜历史
 */
class StoryboardHistoryViewModel : AppRepositoryViewModel<ShortDramaEditorRepository>() {
    val dramasId by lazy { mBundle.getString(Constants.Extra.DRAMAS_ID) }
    val shotSegmentId by lazy { mBundle.getString(Constants.Extra.ID) }
    val shotSegmentImageId by lazy { mBundle.getString(Constants.Extra.IMAGE_ID) }
    val loadState by lazy { ObservableInt() }
    val adapter by lazy { QuickAdapter<ItemStoryboardHistoryVM>(R.layout.item_storyboard_history, emptyList()) }
    val currentItem by lazy { ObservableField<ItemStoryboardHistoryVM>() }

    override fun onCreate() {
        super.onCreate()
        refresh()
    }

    fun refresh() {
        loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                response(
                    bean = mRepo.shortDramasSegmentImageHistory(shotSegmentId),
                    result = {
                        data?.items?.mapIndexed { index, item ->
                            ItemStoryboardHistoryVM(index + 1, item).apply { onItemClick.value = { select() } }
                        }?.let {
                            adapter.submitList(it)
                            it.find { it.data.get()?.shotSegmentImageId == shotSegmentImageId }.select()
                        }
                        if (adapter.itemCount > 0) {
                            loadState.set(LoadStateViewHolder.CONTENT)
                        } else {
                            loadState.set(LoadStateViewHolder.NO_DATA)
                        }
                    },
                    error = {
                        loadState.set(LoadStateViewHolder.RETRY)
                    }
                )
            },
            onError = {
                loadState.set(LoadStateViewHolder.RETRY)
            }
        )
    }

    private fun ItemStoryboardHistoryVM?.select() {
        if (this == currentItem.get()) return
        if (this?.data?.get()?.genStatus != 2) return
        currentItem.get()?.selected?.set(false)
        selected.set(true)
        currentItem.set(this)
    }

    fun use() {
        launchWithLoading {
            response(mRepo.shortDramasSegmentImageReplace(shotSegmentId, currentItem.get()?.data?.get()?.shotSegmentImageId)) {
                "使用成功".showToast()
                dismiss()
                FlowEventBus.postSync(Events.ShortDramasUpdatedEvent(dramasId))
            }
        }
    }

}