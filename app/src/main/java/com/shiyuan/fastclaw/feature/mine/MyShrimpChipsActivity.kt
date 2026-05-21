package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants.SHRIMPSTICKS_URL
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.databinding.ActivityMyShrimpChipsBinding
import com.shiyuan.fastclaw.feature.mine.viewModel.MineViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
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

    // 虾条明细 头部可用虾条
    private val headerAdapter by lazy {
        MyShrimpHeaderAdapter()
    }
    // 虾条明细 明细列表
    private val mListAdapter by lazy {
        MineUserPointAdapter()
    }
    // 对齐iOS效果，组合滚动
    private val mAdapter by lazy {
        ConcatAdapter(headerAdapter, mListAdapter)
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
            // 背景绘制
            addItemDecoration(MyShrimpListDecoration(this@MyShrimpChipsActivity))
            adapter = mAdapter
        }
        mListAdapter.addLoadStateListener { loadStates ->
            // 虾条分页数据状态
            when (loadStates.refresh) {
                is androidx.paging.LoadState.Error -> {

                }
                is androidx.paging.LoadState.Loading -> {

                }
                is androidx.paging.LoadState.NotLoading -> {
                    // tips: 如需空状态缺省图可在ConcatAdapter的headerAdapter与mListAdapter之间加入一个emptyAdapter，在这里控制emptyAdapter的显示隐藏
                    val isEmpty = mListAdapter.itemCount == 0 && loadStates.append.endOfPaginationReached
                }
            }
        }
        mRealVM.onMenuClick = {
            SimpleBrowserActivity.launch(
                this,
                SHRIMPSTICKS_URL
            )
        }

    }

    private fun initObservers() {
        // 监听更新头部数据
        mRealVM.userPoints.observe(this) { value ->
            headerAdapter.updatePoints(value)
        }
        // 监听更新列表数据
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mRealVM.userPointListFlow.collectLatest { list ->
                    mListAdapter.submitData(list)
                }
            }
        }
    }


}
