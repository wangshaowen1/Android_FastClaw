package com.kgmedia.fastfilm.feature.editor.dubbing

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.getAdapterByItemPosition
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.DialogDubbingBinding
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.item.ItemEventsViewModel
import kotlinx.coroutines.launch

/**
 * 短剧配音
 */
class DubbingDialog : BaseBottomPushVMDialogFragment<DialogDubbingBinding, DubbingViewModel>() {
    companion object {
        fun show(fragmentManager: FragmentManager?, dramasId: String?, timbreId: String?) {
            val fragmentManager = fragmentManager ?: (topActivity as? FragmentActivity)?.supportFragmentManager ?: return
            DubbingDialog().apply {
                arguments = Bundle().apply {
                    putString(Constants.Extra.DRAMAS_ID, dramasId)
                    putString(Constants.Extra.TIMBRE, timbreId)
                }
            }.show(fragmentManager)
        }
    }

    private val eventVm by lazy { ItemEventsViewModel<DubbingTimbreEvent>() }
    private val dubbingTimbreAdapter by lazy { DubbingTimbreAdapter(eventVm) }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        observeData()
    }

    private fun initView() {
        val noDataButtonTextAs = "刷新"
        val noDataAs = "暂无数据"
        binding.recyclerView.apply {
            itemAnimator = null
            layoutManager = GridLayoutManager(context, 4).apply {
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
            adapter = dubbingTimbreAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = dubbingTimbreAdapter,
                    noData = noDataAs,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    noDataButtonText = noDataButtonTextAs,
                    displayedHeight = 400.dp,
                    refresh = { refresh() },
                    requestButtonTextAndImg = false,
                    loadMoreLoadEnd = ""
                )
            )
        }
        binding.loadStateViewHolder.apply {
            setRetryListener { refresh() }
            setNoDataButton(noDataButtonTextAs, { refresh() })
            setNoDataPrompt(noDataAs)
        }
        binding.swipeRefreshLayout.startRefreshWithAdapter(dubbingTimbreAdapter)
    }

    private fun refresh() {
        dubbingTimbreAdapter.refresh()
    }

    private fun observeData() {
        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is DubbingTimbreEvent.Click -> {
                        if (this@DubbingDialog.isActive) mRealVM.select(event.item)
                    }
                }
            }
        }
        lifecycleScope.launch {
            mRealVM.timbres().collect {
                dubbingTimbreAdapter.submitData(it)
            }
        }
    }
}