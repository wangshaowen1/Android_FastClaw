package com.kgmedia.fastfilm.feature.invitation.income

import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.FragmentInvitationIncomeBinding
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import kotlinx.coroutines.launch

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请收益列表
 * @Data: 2025/10/13/周一  14:13
 * @Version: 1.0
 * *****************************************
 */
class InvitationIncomeFragment :
    BaseViewModelFragment<FragmentInvitationIncomeBinding, InvitationIncomeViewModel>() {

    companion object {
        fun newInstance(): InvitationIncomeFragment {
            val fragment = InvitationIncomeFragment()
            return fragment
        }
    }

    private val invitationIncomeAdapter by lazy {
        InvitationIncomeAdapter()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        val noDataButtonTextAs = "去邀请"
        val noDataAs = "暂无相关内容，快去邀请吧~"

        binding.recyclerView.apply {
            itemAnimator = null
            adapter = invitationIncomeAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = invitationIncomeAdapter,
                    noData = noDataAs,
                    noDataButtonDisplay = true,
                    requestButtonDisplay = false,
                    noDataButtonText = noDataButtonTextAs,
                    refresh = {
                        notDataHandle()
                    },
                    requestButtonTextAndImg = false,
                )
            )
        }
        binding.lsv.apply {
            setRetryListener {
                //刷新
                invitationIncomeAdapter.refresh()
            }
            setNoDataButton(noDataButtonTextAs, {
                notDataHandle()
            })
            setNoDataPrompt(noDataAs)
        }

        binding.srl.startRefreshWithAdapter(invitationIncomeAdapter)
    }

    fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.invitationIncome().collect {
                invitationIncomeAdapter.submitData(it)
            }
        }
    }

    private fun notDataHandle() {
        //没有数据，去邀请
        topActivity?.finish()
    }
}