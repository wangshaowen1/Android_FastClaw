package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showAllowingStateLoss
import com.nhsd.basic.viewmodel.UiState
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ActivityMyShrimpChipsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @Introduce: 我的虾条
 */
class MyShrimpChipsActivity : BaseViewModelActivity<ActivityMyShrimpChipsBinding, MineViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, MyShrimpChipsActivity::class.java)
            activity.startActivity(intent)
        }
    }


    private val mAdapter by lazy {
        MineUserPointAdapter()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
        initObservers()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyShrimpChipsActivity, RecyclerView.VERTICAL, false)
            adapter = mAdapter
        }
        mAdapter.addLoadStateListener { loadStates ->
            // 虾条分页数据状态
            when (loadStates.refresh) {
                is androidx.paging.LoadState.Error -> {

                }
                is androidx.paging.LoadState.Loading -> {

                }
                is androidx.paging.LoadState.NotLoading -> {
                    val isEmpty = mAdapter.itemCount == 0 && loadStates.append.endOfPaginationReached
                }
            }
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            mRealVM.testLoading()
            mRealVM.userPointListFlow.collectLatest { list ->
                mAdapter.submitData(list)
            }
        }
    }


}
