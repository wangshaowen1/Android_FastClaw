package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants.SHRIMPSTICKS_URL
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
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
            EventManager.onAppTrack(AppEvent.KLX_CAPP_MY_SHRIMPSTICK_PAGE)
        }
    }

    private val mListAdapter by lazy {
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
            adapter = mListAdapter
        }
        mRealVM.onMenuClick = {
            SimpleBrowserActivity.launch(
                this,
                SHRIMPSTICKS_URL
            )
        }
        setRuleTipText()
        binding.tvRuleTip.setOnSingleClickListener {
            mRealVM.onMenuClick?.invoke()
        }

    }

    private fun initObservers() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mRealVM.userPointListFlow.collectLatest { list ->
                    mListAdapter.submitData(list)
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
