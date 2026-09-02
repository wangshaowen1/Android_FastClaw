package com.shiyuan.fastclaw.feature.opportunity

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.common.ext.startRefreshWithAdapter
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.ext.onUserStatusChanged
import com.shiyuan.fastclaw.core.network.apis.response.OpportunityProcessedItemData
import com.shiyuan.fastclaw.databinding.FragmentProcessedOpportunityListBinding
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.shiyuan.fastclaw.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 已处理机会的单个状态列表页。
 *
 * 通过 [OpportunityViewModel.processedOpportunityPagingFlow] 请求分页列表，
 * 并通过聚合响应刷新“沟通中”和“不合适”两个 Tab 的总数。
 */
class ProcessedOpportunityListFragment :
    BaseViewModelFragment<FragmentProcessedOpportunityListBinding, OpportunityViewModel>() {

    companion object {
        private const val ARG_TAB_TYPE = "arg_tab_type"
        private const val TAB_COMMUNICATING = 0
        private const val TAB_UNSUITABLE = 1

        /**
         * 创建指定状态的列表页。
         *
         * @param tabType 0 为沟通中，1 为不合适。
         */
        fun newInstance(tabType: Int): ProcessedOpportunityListFragment =
            ProcessedOpportunityListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TAB_TYPE, tabType)
                }
            }
    }

    /** 分页列表适配器，点击条目进入对应岗位的机会详情。 */
    private val opportunityAdapter by lazy {
        ProcessedOpportunityPagingAdapter { opportunity ->
            OpportunityDetailActivity.start(requireActivity(), opportunity.jobId)
        }
    }

    /** 分页尾部状态，用于展示加载中、失败和没有更多数据。 */
    private val footerAdapter by lazy {
        MyMultiStatePagingFooterAdapter(
            hostAdapter = opportunityAdapter,
            displayOnEmpty = false
        )
    }

    private val tabType by lazy {
        requireArguments().getInt(ARG_TAB_TYPE)
    }

    /** 当前 Tab 对应的服务端机会状态。 */
    private val status by lazy {
        if (tabType == TAB_COMMUNICATING) {
            OpportunityProcessedItemData.STATUS_COMMUNICATING
        } else {
            OpportunityProcessedItemData.STATUS_UNSUITABLE
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        observeEvents {
            on<Events.OpportunityListRefreshEvent> {
                opportunityAdapter.refresh()
                mRealVM.queryProcessedOpportunityCounts(status)
            }
        }
        binding.rvProcessedOpportunity.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            itemAnimator = null
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(10.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = opportunityAdapter.withLoadStateFooter(footerAdapter)
        }
        // 下拉刷新由 Paging3 重新加载，同时刷新服务端返回的两个状态总数。
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = opportunityAdapter,
            loadStateHolder = binding.lsvProcessedOpportunity,
            onRefreshListener = {
                mRealVM.queryProcessedOpportunityCounts(status)
            }
        )
        // 返回 OpportunityProcessedPageData，两个列表页共享 ViewModel 中的聚合计数。
        mRealVM.processedOpportunityPageData.observe(viewLifecycleOwner) { data ->
            (activity as? ProcessedOpportunityActivity)?.apply {
                updateTabCount(TAB_COMMUNICATING, data?.communicatingCount ?: 0)
                updateTabCount(TAB_UNSUITABLE, data?.unsuitableCount ?: 0)
            }
        }
        // PagingData 随页面 STARTED 生命周期收集，停止可见后自动取消本次收集。
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
