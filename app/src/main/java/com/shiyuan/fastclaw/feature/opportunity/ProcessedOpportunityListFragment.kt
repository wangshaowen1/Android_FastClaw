package com.shiyuan.fastclaw.feature.opportunity

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.databinding.FragmentProcessedOpportunityListBinding
import com.shiyuan.fastclaw.feature.opportunity.vm.OpportunityViewModel
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration

class ProcessedOpportunityListFragment :
    BaseViewModelFragment<FragmentProcessedOpportunityListBinding, OpportunityViewModel>() {

    companion object {
        private const val ARG_TAB_TYPE = "arg_tab_type"
        private const val TAB_COMMUNICATING = 0

        fun newInstance(tabType: Int): ProcessedOpportunityListFragment =
            ProcessedOpportunityListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TAB_TYPE, tabType)
                }
            }
    }

    private val opportunityAdapter by lazy {
        QuickAdapter<ItemProcessedOpportunityVM>(
            R.layout.item_processed_opportunity,
            emptyList()
        )
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.rvProcessedOpportunity.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            if (itemDecorationCount == 0) {
                addItemDecoration(SpaceDecoration(10.dp, SpaceDecoration.Direction.MIDDLE))
            }
            adapter = opportunityAdapter
        }
        binding.srl.setOnRefreshListener {
            mRealVM.chanceData.value = mRealVM.createFakeChanceData()
        }
        binding.lsvProcessedOpportunity.showLoading()
        mRealVM.chanceData.value = mRealVM.createFakeChanceData()
        mRealVM.chanceData.observe(viewLifecycleOwner) { data ->
            binding.srl.finishRefresh()
            val tabType = requireArguments().getInt(ARG_TAB_TYPE)
            val items = if (tabType == TAB_COMMUNICATING) {
                data.orEmpty()
            } else {
                emptyList()
            }
            opportunityAdapter.submitList(items.map(::ItemProcessedOpportunityVM))
            if (items.isEmpty()) {
                binding.lsvProcessedOpportunity.showEmpty()
            } else {
                binding.lsvProcessedOpportunity.showContent()
            }
            (activity as? ProcessedOpportunityActivity)?.updateTabCount(tabType, items.size)
        }
    }
}

data class ProcessedOpportunityItem(
    val title: String,
    val company: String,
    val requirements: String,
    val salary: String,
    val actionTime: String,
    val matchPercent: Int
)

class ItemProcessedOpportunityVM(data: ProcessedOpportunityItem) : AppItemViewModel<ProcessedOpportunityItem>(data)
