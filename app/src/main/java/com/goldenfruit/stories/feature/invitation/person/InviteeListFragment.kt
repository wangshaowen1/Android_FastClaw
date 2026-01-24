package com.kgmedia.fastfilm.feature.invitation.person

import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.FragmentInviteeListBinding
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import kotlinx.coroutines.launch

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请人列表
 * @Data: 2025/10/13/周一  15:24
 * @Version: 1.0
 * *****************************************
 */
class InviteeListFragment :
    BaseViewModelFragment<FragmentInviteeListBinding, InviteeListViewModel>() {
    companion object {
        fun newInstance(): InviteeListFragment {
            val fragment = InviteeListFragment()
            return fragment
        }
    }

    private val inviteeListAdapter by lazy {
        InviteeListAdapter()
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
            adapter = inviteeListAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = inviteeListAdapter,
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
                inviteeListAdapter.refresh()
            }
            setNoDataButton(noDataButtonTextAs, {
                notDataHandle()
            })
            setNoDataPrompt(noDataAs)
        }

        binding.srl.startRefreshWithAdapter(inviteeListAdapter)
    }

    fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.inviteeList().collect {
                inviteeListAdapter.submitData(it)
            }
        }
    }

    private fun notDataHandle() {
        //没有数据，去邀请
        topActivity?.finish()
    }
}