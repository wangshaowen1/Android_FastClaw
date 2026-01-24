package com.kgmedia.fastfilm.feature.play

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.FooterAdapter
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.PlaylistDataTransfer
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.DramasSeriesInfo
import com.kgmedia.fastfilm.databinding.DialogShortSeriesBinding
import com.kgmedia.fastfilm.feature.series.SeriesActivity
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.getViewModelOfParentFragment
import com.nhsd.basic.item.ItemEventsViewModel
import kotlinx.coroutines.launch

/**
 * 剧集选择
 */
class SeriesShortsDialog : BaseBottomPushVMDialogFragment<DialogShortSeriesBinding, SeriesShortsViewModel>() {

    companion object {
        fun show(
            manager: FragmentManager,
            seriesInfo: DramasSeriesInfo?,
            playerInSeries: Boolean = false
        ): SeriesShortsDialog {
            val dialog = SeriesShortsDialog()
            dialog.arguments = Bundle().apply {
                putParcelable(Constants.Extra.DATA, seriesInfo)
                putBoolean(Constants.Extra.PLAYER_IN_SERIES, playerInSeries)
            }
            dialog.show(manager)
            return dialog
        }
    }

    val parentVm by lazy { parentFragment?.getViewModelOfParentFragment<ShortDramaPlayPagerViewModel>() ?: getViewModelOfActivity() }

    private val eventVM by lazy { ItemEventsViewModel<ShortClickEvent>() }

    val shortsAdapter by lazy { SeriesShortsAdapter(eventVM, player = parentVm?.shortsPlayer?.delegate?.player) }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        mRealVM.playlistPlayer = parentVm?.shortsPlayer
    }

    var unTotalWorkNumChange: ((Int) -> Unit)? = null

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onAppTrack(AppEvent.JG_VIEW_SERIES_PAGE)
        mRealVM.unTotalWorkNumChange = {
            unTotalWorkNumChange?.invoke(it)
        }
        binding.playlistView.apply {
            layoutManager = LinearLayoutManager(mActivity)
            isFocusable = false
//            addItemDecoration(
//                LinearLayoutManagerSpacingDecoration(
//                    10, 10,
//                    LinearLayoutManagerSpacingDecoration.Orientation.Vertical
//                )
//            )
            adapter = shortsAdapter.withLoadStateFooter(FooterAdapter { shortsAdapter.retry() })
        }
        binding.loadStateViewHolder.startRefreshWithAdapter(shortsAdapter)
        lifecycleScope.launch {
            mRealVM.seriesShortDramas()?.collect {
                shortsAdapter.submitData(it)
            }
        }
        lifecycleScope.launch {
            eventVM.events.collect { event ->
                when (event) {
                    is ShortClickEvent.ItemClick -> {
                        if (mRealVM.playerInSeries) {
                            parentVm?.checkedShort?.value = event.short
                        } else {
                            val data = shortsAdapter.snapshot().items
                            parentVm?.pausePlayerBeforeSkip()
                            // 2. 存入传输工具
                            PlaylistDataTransfer.put(data)
                            ShortDramaPlayPagerActivity.start(mRealVM.seriesInfo?.dramasSeriesId, event.position, true)
                        }
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
        parentVm?.shortsPlayer?.addListener(shortsAdapter)
        observeEvent()
    }

    fun observeEvent() {
        observeEvents {
            on<Events.UnLockEvent> { event ->
                shortsAdapter.snapshot().items.find { it.dramasId == event.dramasId }?.unlockInfo?.unlockStatus = 1
                shortsAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.seriesInfo -> {
                SeriesActivity.start(mRealVM.seriesInfo?.dramasSeriesId)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        parentVm?.shortsPlayer?.removeListener(shortsAdapter)
    }

}