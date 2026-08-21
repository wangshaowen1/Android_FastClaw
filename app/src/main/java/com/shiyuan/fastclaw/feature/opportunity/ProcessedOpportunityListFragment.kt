package com.shiyuan.fastclaw.feature.opportunity

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.common.ext.startRefreshWithAdapter
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityProcessedItemData
import com.shiyuan.fastclaw.databinding.FragmentProcessedOpportunityListBinding
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityViewModel
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.shiyuan.fastclaw.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProcessedOpportunityListFragment :
    BaseViewModelFragment<FragmentProcessedOpportunityListBinding, OpportunityViewModel>() {

    companion object {
        private const val ARG_TAB_TYPE = "arg_tab_type"
        private const val TAB_COMMUNICATING = 0
        private const val TAB_UNSUITABLE = 1

        fun newInstance(tabType: Int): ProcessedOpportunityListFragment =
            ProcessedOpportunityListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TAB_TYPE, tabType)
                }
            }
    }

    private val opportunityAdapter by lazy {
        ProcessedOpportunityPagingAdapter { opportunity ->
            OpportunityDetailActivity.start(requireActivity(), opportunity.jobId)
        }
    }

    private val footerAdapter by lazy {
        MyMultiStatePagingFooterAdapter(
            hostAdapter = opportunityAdapter,
            displayOnEmpty = false
        )
    }

    private val tabType by lazy {
        requireArguments().getInt(ARG_TAB_TYPE)
    }

    private val status by lazy {
        if (tabType == TAB_COMMUNICATING) {
            OpportunityProcessedItemData.STATUS_COMMUNICATING
        } else {
            OpportunityProcessedItemData.STATUS_UNSUITABLE
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.rvProcessedOpportunity.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(10.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = opportunityAdapter.withLoadStateFooter(footerAdapter)
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = opportunityAdapter,
            loadStateHolder = binding.lsvProcessedOpportunity,
            onRefreshListener = {
                mRealVM.queryProcessedOpportunityCounts(status)
            }
        )
        mRealVM.processedOpportunityPageData.observe(viewLifecycleOwner) { data ->
            (activity as? ProcessedOpportunityActivity)?.apply {
                updateTabCount(TAB_COMMUNICATING, data?.communicatingCount ?: 0)
                updateTabCount(TAB_UNSUITABLE, data?.unsuitableCount ?: 0)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mRealVM.processedOpportunityPagingFlow(status).collectLatest { pagingData ->
                    opportunityAdapter.submitData(pagingData)
                }
            }
        }
        mRealVM.queryProcessedOpportunityCounts(status)
    }
}
