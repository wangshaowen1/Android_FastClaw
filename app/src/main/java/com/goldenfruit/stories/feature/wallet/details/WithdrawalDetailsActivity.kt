package com.kgmedia.fastfilm.feature.wallet.details

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.ActivityWithdrawalDetailsBinding
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.launch

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:提现明细
 * @Data: 2025/10/14/周二  9:57
 * @Version: 1.0
 * *****************************************
 */
class WithdrawalDetailsActivity :
    BaseViewModelActivity<ActivityWithdrawalDetailsBinding, WithdrawalDetailsViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            val activity = activity ?: topActivity ?: return
            val intent = Intent(activity, WithdrawalDetailsActivity::class.java)
            activity.startActivity(intent)
        }
    }

    private val withdrawalDetailsAdapter by lazy {
        WithdrawalDetailsAdapter()
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        //更新数据
        updateData()
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(12.dp))
            itemAnimator = null
            adapter = withdrawalDetailsAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = withdrawalDetailsAdapter,
                    refresh = {
                        withdrawalDetailsAdapter.retry()
                    },
                )
            )
        }
        binding.srl.startRefreshWithAdapter(withdrawalDetailsAdapter)
    }

    private fun updateData() {
        lifecycleScope.launch {
            mRealVM.withdrawalDetails().collect {
                withdrawalDetailsAdapter.submitData(it)
            }
        }
    }
}