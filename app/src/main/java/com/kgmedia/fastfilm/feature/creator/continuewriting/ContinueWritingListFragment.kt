package com.kgmedia.fastfilm.feature.creator.continuewriting

import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.databinding.FragmentContinueWritingListBinding
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.launch

class ContinueWritingListFragment : BaseViewModelFragment<FragmentContinueWritingListBinding, ContinueWritingListViewModel>() {
    companion object {
        fun newInstance(): ContinueWritingListFragment {
            val fragment = ContinueWritingListFragment()
            return fragment
        }
    }

    val continueWritingAdapter by lazy {
        ContinueWritingAdapter()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        val noDataAs = "您还没有发布小说\n快去发布吧~"
        binding.recyclerView.apply {
//            addItemDecoration(SpaceDecoration(12.dp))
            itemAnimator = null
            adapter = continueWritingAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = continueWritingAdapter,
                    noData = noDataAs,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    displayedHeight = 400.dp,
                    refresh = {
                        continueWritingAdapter.refresh()
                    },
                    requestButtonTextAndImg = false,
                )
            )
        }
        binding.lsv.apply {
            makeScrollable()
            setRetryListener {
                //刷新
                continueWritingAdapter.refresh()
            }
            setNoDataPrompt(noDataAs)
        }

        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = continueWritingAdapter
        )
    }

    fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.dataGet.collect {
                continueWritingAdapter.submitData(it)
            }
        }
    }
}