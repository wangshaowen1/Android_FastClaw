package com.kgmedia.fastfilm.feature.message.likes

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.databinding.ActivitySystemLikesBinding
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.launch

/**
 * 系统通知
 */
class SystemMessageLikesActivity : BaseViewModelActivity<ActivitySystemLikesBinding, SystemLikesViewModel>() {

    private val messageAdapter by lazy { SystemMessageLikesAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeData()
    }

    private fun initView() {
        val noDataButtonTextAs = "刷新"
        val noDataAs = "还没有人喜欢你的作品哦~"
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
            setNoDataIcon(R.drawable.ic_empty_like)
            setNoDataPrompt(noDataAs)
        }
        binding.swipeRefreshLayout.startRefreshWithAdapter(messageAdapter)
    }

    private fun refresh() {
        messageAdapter.refresh()
    }

    private fun observeData() {
        lifecycleScope.launch {
            mRealVM.msgFlow().collect {
                messageAdapter.submitData(it)
            }
        }
    }
}