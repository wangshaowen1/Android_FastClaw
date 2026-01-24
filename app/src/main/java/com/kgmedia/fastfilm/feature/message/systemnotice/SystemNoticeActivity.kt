package com.kgmedia.fastfilm.feature.message.systemnotice

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.databinding.ActivitySystemNoticeBinding
import com.kgmedia.fastfilm.feature.PhotoPagerActivity
import com.kgmedia.fastfilm.router.AppRouter
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.toArrayList
import com.nhsd.basic.item.ItemEventsViewModel
import kotlinx.coroutines.launch

/**
 * 系统通知
 */
class SystemNoticeActivity : BaseViewModelActivity<ActivitySystemNoticeBinding, SystemNoticeViewModel>() {

    private val messageAdapter by lazy { SystemNoticeAdapter(eventVM) }
    private val eventVM by lazy { ItemEventsViewModel<ItemSystemNoticeEvent>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeData()
    }

    private fun initView() {
        val noDataButtonTextAs = "刷新"
        val noDataAs = "暂无数据"
        binding.recyclerView.apply {
            itemAnimator = null
            adapter = messageAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = messageAdapter,
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
            setGravityHorizontalTop(210.dp)
            setRetryListener { refresh() }
            setNoDataButton(noDataButtonTextAs, { refresh() })
            setNoDataPrompt(noDataAs)
        }
        binding.swipeRefreshLayout.startRefreshWithAdapter(messageAdapter)
    }

    private fun refresh() {
        messageAdapter.refresh()
    }

    private fun observeData() {
        lifecycleScope.launch {
            eventVM.events.collect { event ->
                when (event) {
                    is ItemSystemNoticeEvent.Click -> {
                        val jumpUri = event.systemNoticeInfo?.jumpUri
                        val imageList = event.systemNoticeInfo?.imageList?.toArrayList()
                        if (!jumpUri.isNullOrEmpty()) {
                            AppRouter.jump(jumpUri)
                        } else if (!imageList.isNullOrEmpty()) {
                            PhotoPagerActivity.launch(this@SystemNoticeActivity, imageList, 0)
                        }
                    }

                    is ItemSystemNoticeEvent.ImageClick -> {
                        val imageList = event.systemNoticeInfo?.imageList?.toArrayList() ?: return@collect
                        val index = imageList.indexOf(event.imageUrl).coerceAtLeast(0)
                        PhotoPagerActivity.launch(this@SystemNoticeActivity, imageList, index)
                    }
                }
            }
        }
        lifecycleScope.launch {
            mRealVM.systemNoticeFlow().collect {
                messageAdapter.submitData(it)
            }
        }
    }
}