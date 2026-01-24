package com.kgmedia.fastfilm.feature.mine.like

import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.getAdapterByItemPosition
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.onUserStatusChanged
import com.kgmedia.fastfilm.databinding.FragmentLikeBinding
import com.kgmedia.fastfilm.feature.mine.MineViewModel
import com.kgmedia.fastfilm.feature.play.ShortDramaPlayerActivity
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
 * @Introduce:喜欢
 * @Data: 2025/11/4/周二  15:30
 * @Version: 1.0
 * *****************************************
 */
class LikeFragment : BaseViewModelFragment<FragmentLikeBinding, LikeViewModel>() {

    companion object {

        fun newInstance(): LikeFragment {
            val fragment = LikeFragment()
            return fragment
        }
    }

    private var userId: String? = null
    private val eventVm by lazy {
        ItemEventsViewModel<LikeEvent>()
    }
    private val likeAdapter by lazy {
        LikeAdapter(eventVm)
    }

    override fun onPrepareView() {
        super.onPrepareView()
        userId = User.get().id

        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.recyclerView.apply {
            itemAnimator = null
            layoutManager = GridLayoutManager(context, 3).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        val adapter = binding.recyclerView.adapter
                        if (adapter is ConcatAdapter) {
                            if (adapter.getAdapterByItemPosition(position) is LoadStateAdapter) {
                                return spanCount
                            }
                        }
                        return 1
                    }
                }
            }
            adapter = likeAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = likeAdapter,
                    noData = "你还没有喜欢的作品",
                    noDataSrc = R.drawable.ic_no_data_like,
                    displayedHeight = 400.dp,
                    refresh = {
                        likeAdapter.retry()
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
        }
        binding.srl.startRefreshWithAdapter(likeAdapter)
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is LikeEvent.ItemClick -> {
                        ShortDramaPlayerActivity.start(event.myFavoriteScriptInfo?.dramasId)
                    }
                }
            }
        }

        lifecycleScope.launch {
            mRealVM.like().collect {
                likeAdapter.submitData(it)
            }
        }
        lifecycleScope.launch {
            mRealVM.totalCount.collect {
                getViewModelOfParentFragment<MineViewModel>()?.like?.value = it
            }
        }
        observeEvents {
            /**
             * 登录状态变化
             */
            onUserStatusChanged {
                if (userId == User.get().id) return@onUserStatusChanged
                userId = User.get().id
                refresh()
            }
            on<Events.ShortDramaLikeEvent> { event ->
                refresh()
            }
        }
    }

    private fun refresh() {
        likeAdapter.refresh()
    }
}