package com.kgmedia.fastfilm.feature.mine.episode

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.onUserStatusChanged
import com.kgmedia.fastfilm.databinding.FragmentEpisodeListBinding
import com.kgmedia.fastfilm.feature.series.SeriesActivity
import com.kgmedia.fastfilm.feature.series.share.SeriesShareDialog
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfParentFragment
import com.nhsd.basic.item.ItemEventsViewModel
import kotlinx.coroutines.launch

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:小说列表
 * @Data: 2025/11/4/周二  17:50
 * @Version: 1.0
 * *****************************************
 */
class EpisodeListFragment :
    BaseViewModelFragment<FragmentEpisodeListBinding, EpisodeListViewModel>() {

    companion object {
        //类型
        const val LISTING_TYPE = "listingType"

        //已上架
        const val LISTING_TYPE_LAUNCH = 0

        //未上架
        const val LISTING_TYPE_NOT_LAUNCH = 1

        fun newInstance(listingType: Int = LISTING_TYPE_LAUNCH): EpisodeListFragment {
            val fragment = EpisodeListFragment()
            val bundle = Bundle()
            bundle.putInt(LISTING_TYPE, listingType)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var userId: String? = null
    private val eventVm by lazy {
        ItemEventsViewModel<EpisodeListEvent>()
    }
    private val episodeListAdapter by lazy {
        EpisodeListAdapter(eventVm, mRealVM.listingType)
    }

    override fun onPrepareView() {
        super.onPrepareView()
        userId = User.get().id

        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        val noData = if (mRealVM.listingType == LISTING_TYPE_LAUNCH) {
            "你还没有上架小说哦～"
        } else {
            "你还没有发布小说哦～"
        }

        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(10.dp))
            itemAnimator = null
            adapter = episodeListAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = episodeListAdapter,
                    noData = noData,
                    displayedHeight = 400.dp,
                    refresh = {
                        episodeListAdapter.retry()
                    },
                    requestButtonTextAndImg = false,
                )
            )
        }
        binding.lsv.apply {
            makeScrollable()
            setRetryListener {
                //刷新
                refresh()
            }
            setNoDataPrompt(noData)
        }
        binding.srl.startRefreshWithAdapter(episodeListAdapter)
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is EpisodeListEvent.ItemClick -> {
                        //点击查看
                        SeriesActivity.start(event.myTvSeriesListInfo?.dramasSeriesId)
                    }

                    is EpisodeListEvent.Launch -> {
                        //上架
                        SeriesActivity.start(event.myTvSeriesListInfo?.dramasSeriesId)
                    }

                    is EpisodeListEvent.Share -> {
                        //分享
                        event.myTvSeriesListInfo?.apply {
                            SeriesShareDialog.show(childFragmentManager, User.get().id, dramasSeriesId, seriesName, seriesCoverUrl)
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            mRealVM.episodeList().collect {
                episodeListAdapter.submitData(it)
            }
        }
        lifecycleScope.launch {
            mRealVM.totalCount.collect {
                getViewModelOfParentFragment<EpisodeViewModel>()?.apply {
                    if (mRealVM.listingType == LISTING_TYPE_LAUNCH) {
                        totalCount.value = it
                    } else {
                        totalCountNotLaunch.value = it
                    }
                }
            }
        }
        observeEvents {
            /**
             * 登录状态变化
             */
            onUserStatusChanged {
                if (userId == User.get().id) return@onUserStatusChanged
                userId = User.get().id
                if (User.get().isLogged) {
                    refresh()
                }
            }
            on<Events.SeriesUpdatedEvent> { event ->
                refresh()
            }
        }
    }


    private fun refresh() {
        episodeListAdapter.refresh()
    }
}