package com.shiyuan.fastclaw.feature.mine.fragment

import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants.SHRIMPSTICKS_URL
import com.shiyuan.fastclaw.common.ext.startRefreshWithAdapter
import com.shiyuan.fastclaw.databinding.FragmentMyShrimpChipsBinding
import com.shiyuan.fastclaw.feature.mine.MineUserPointAdapter
import com.shiyuan.fastclaw.feature.mine.viewModel.MineViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyShrimpChipsFragment :
    BaseViewModelFragment<FragmentMyShrimpChipsBinding, MineViewModel>() {

    companion object {
        fun newInstance(): MyShrimpChipsFragment = MyShrimpChipsFragment()
    }

    private val listAdapter by lazy {
        MineUserPointAdapter()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObservers()
    }

    private fun initView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = listAdapter
        }
        binding.srl.startRefreshWithAdapter(listAdapter)
        setRuleTipText()
        binding.tvRuleTip.setOnSingleClickListener {
            SimpleBrowserActivity.launch(requireActivity(), SHRIMPSTICKS_URL)
        }
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mRealVM.userPointListFlow.collectLatest { list ->
                    listAdapter.submitData(list)
                }
            }
        }
    }

    private fun setRuleTipText() {
        val tip = getString(R.string.mine_shrimp_rule_tip)
        val rule = getString(R.string.mine_rule)
        val start = tip.indexOf(rule)
        if (start < 0) {
            binding.tvRuleTip.text = tip
            return
        }
        binding.tvRuleTip.text = SpannableString(tip).apply {
            setSpan(
                UnderlineSpan(),
                start,
                start + rule.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
