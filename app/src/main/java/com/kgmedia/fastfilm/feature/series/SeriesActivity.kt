package com.kgmedia.fastfilm.feature.series

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.PlaylistDataTransfer
import com.kgmedia.fastfilm.databinding.ActivitySeriesBinding
import com.kgmedia.fastfilm.feature.creator.continuewriting.ContinueWritingActivity
import com.kgmedia.fastfilm.feature.play.DramasPlayPagerActivity
import com.kgmedia.fastfilm.feature.series.editinfo.EditInfoActivity
import com.kgmedia.fastfilm.feature.series.more.SeriesMoreDialog
import com.kgmedia.fastfilm.feature.series.setprice.SetPriceDialog
import com.kgmedia.fastfilm.widget.decoration.BottomSpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.item.ItemEventsViewModel
import kotlinx.coroutines.launch

/**
 * 剧集详情页
 */
class SeriesActivity : BaseViewModelActivity<ActivitySeriesBinding, SeriesViewModel>() {
    companion object {
        fun start(seriesId: String?, newlyPublish: Boolean = false) {
            val activity = topActivity ?: return
            activity.startActivity(
                Intent(activity, SeriesActivity::class.java).apply {
                    putExtra(Constants.Extra.ID, seriesId)
                    putExtra(Constants.Extra.NEWLY_PUBLISH, newlyPublish)
                }
            )
        }
    }

    private val seriesId by lazy { intent.getStringExtra(Constants.Extra.ID) }
    private val eventVm by lazy { ItemEventsViewModel<SeriesEpisodeEvent>() }
    private val episodeAdapter by lazy { SeriesEpisodeAdapter(eventVm) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbarLayout.minimumHeight = 32.dp
        initView()
        observeData()
    }

    private fun initView() {
        val noDataButtonTextAs = "刷新"
        val noDataAs = "暂无数据"
        binding.recyclerView.apply {
            itemAnimator = null
            addItemDecoration(BottomSpaceDecoration(80.dp))
            adapter = episodeAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = episodeAdapter,
                    noData = noDataAs,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    noDataButtonText = noDataButtonTextAs,
                    displayedHeight = 400.dp,
                    refresh = { refresh() },
                    requestButtonTextAndImg = false,
                )
            )
        }
        binding.loadStateViewHolder.apply {
            setGravityHorizontalTop(100.dp)
            setRetryListener { refresh() }
            setNoDataButton(noDataButtonTextAs, { refresh() })
            setNoDataPrompt(noDataAs)
        }
        binding.swipeRefreshLayout.startRefreshWithAdapter(episodeAdapter)
    }

    private fun refresh() {
        episodeAdapter.refresh()
    }

    private fun observeData() {
        mRealVM.autoSetPrice.observe(this) {
            if (it == 2) {
                mRealVM.autoSetPrice.value = 1
                SetPriceDialog.show(supportFragmentManager, seriesId, true)
            }
        }
        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is SeriesEpisodeEvent.Click -> {
                        val snapshot = episodeAdapter.snapshot().items
                        // 只有当 snapshot 包含我们要跳的位置时，才使用 DataTransfer
                        // 如果 snapshot 数据不全（因为 Paging 回收），则不传数据，让播放页自己去拉取全量/定位
                        if (event.position < snapshot.size) {
                            // 注意：这里假设 snapshot 是从 0 开始的连续列表。
                            // 如果 Paging 使用了 Placeholders (占位符)，snapshot.size 会很大，那是安全的。
                            // 如果没有 Placeholders 且 Drop 了页面，这里比较复杂。
                            PlaylistDataTransfer.put(snapshot)
                        } else {
                            // 如果位置极其靠后且前面的数据被 Drop 了，还是让播放页自己去网络请求比较稳妥
                            PlaylistDataTransfer.put(null)
                        }
                        DramasPlayPagerActivity.start(seriesId)
                    }
                }
            }
        }
        lifecycleScope.launch {
            mRealVM.episodeList().collect {
                episodeAdapter.submitData(it)
            }
        }
        observeEvents {
            on<Events.SeriesUpdatedEvent> { event ->
                if (isActive && event.seriesId == seriesId) {
                    mRealVM.getSeriesInfo()
                }
            }
            on<Events.ShortDramasEditInfoEvent> { event ->
                if (episodeAdapter.snapshot().any { it?.dramasId == event.dramaId }) {
                    refresh()
                }
            }
            on<Events.UnLockEvent> { event ->
                episodeAdapter.snapshot().items.find { it.dramasId == event.dramasId }?.unlockInfo?.unlockStatus = 1
                episodeAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnEdit -> {
                EditInfoActivity.start(seriesId)
            }

            R.id.btnMore -> {
                val series = mRealVM.series.get() ?: return
                SeriesMoreDialog.show(null, seriesId, series.launchStatus == 1)
            }

            R.id.btnContinue -> {
                mRealVM.series.get()?.let {
                    ContinueWritingActivity.start(this, seriesId, it.seriesName)
                }
            }
        }
    }

}