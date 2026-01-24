package com.kgmedia.fastfilm.feature.hit.tutorial

import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.databinding.FragmentTutorialChildBinding
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.launchNoCrash

class TutorialChildFragment : BaseViewModelFragment<FragmentTutorialChildBinding, TutorialChildViewModel>() {
    companion object {
        fun newInstance(): TutorialChildFragment {
            val fragment = TutorialChildFragment()
            return fragment
        }
    }

    val tutorialAdapter by lazy {
        TutorialAdapter().apply {
            onAdapterItemClick = {
                EventManager.onAppTrack(
                    AppEvent.JG_CLICK_FADDISH_PAGE_CONSULT_STRATEGY_CELL,
                    GrowingIO.KEY_ALL_GENERAL_ID to it?.strategyInfoId
                )
                mRealVM.clickReport(it)
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.recyclerView.apply {
            isFocusable = false
            adapter = tutorialAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = tutorialAdapter,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    displayedHeight = 400.dp,
                    refresh = {
                        tutorialAdapter.refresh()
                    },
                    requestButtonTextAndImg = false,
                )
            )
            doOnLayout {
                scrollToPosition(0)
            }
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = tutorialAdapter
        )

        binding.lsv.apply {
            makeScrollable()
            setRetryListener {
                //刷新
                tutorialAdapter.refresh()
            }
        }
    }

    fun dataUpdate() {
        lifecycleScope.launchNoCrash {
            mRealVM.dataGet.collect {
                tutorialAdapter.submitData(it)
            }
        }
    }
}