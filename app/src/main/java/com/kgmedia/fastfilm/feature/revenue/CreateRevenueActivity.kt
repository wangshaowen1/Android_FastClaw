package com.kgmedia.fastfilm.feature.revenue

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.databinding.ActivityCreateRevenueBinding
import com.kgmedia.fastfilm.feature.wallet.withdraw.WithdrawActivity
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.launch

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:创造收益
 * @Data: 2025/11/4/周二  10:57
 * @Version: 1.0
 * *****************************************
 */
class CreateRevenueActivity :
    BaseViewModelActivity<ActivityCreateRevenueBinding, CreateRevenueViewModel>() {
    companion object {

        fun start(
            activity: Activity? = null,
        ) {
            val context = activity ?: topActivity ?: return
            val intent = Intent(context, CreateRevenueActivity::class.java)
            context.startActivity(intent)
        }
    }

    private val createRevenueAdapter by lazy {
        CreateRevenueAdapter()
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(12.dp))
            itemAnimator = null
            adapter = createRevenueAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = createRevenueAdapter,
                    refresh = {
                        createRevenueAdapter.retry()
                    },
                )
            )
        }
        binding.srl.startRefreshWithAdapter(
            createRevenueAdapter,
            onRefreshListener = {
                mRealVM.userRevenueInformation()
            })
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.createRevenue().collect {
                createRevenueAdapter.submitData(it)
            }
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.iv_withdraw -> {
                //去提现
                WithdrawActivity.start(this)
            }
        }
    }
}