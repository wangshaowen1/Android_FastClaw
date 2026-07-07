package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.FragmentStateCacheAdapter
import com.shiyuan.fastclaw.databinding.ActivityOnlineResumeBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import com.shiyuan.fastclaw.feature.mine.fragment.JobPreferencesFragment
import com.shiyuan.fastclaw.feature.mine.fragment.ResumeInfoFragment
import com.shiyuan.fastclaw.feature.mine.viewModel.UnderstandsViewModel

/**
 * @Introduce: 小虾理解
 */

class OnlineResumeActivity :
    BaseViewModelActivity<ActivityOnlineResumeBinding, InformationViewModel>() {

    companion object {
        private const val EXTRA_SELECTED_INDEX = "extra_selected_index"
        fun start(activity: Activity?, selectedIndex: Int = 0) {
            activity ?: return
            val intent = Intent(activity, OnlineResumeActivity::class.java).apply {
                putExtra(EXTRA_SELECTED_INDEX, selectedIndex.coerceIn(0, 1))
            }
            activity.startActivity(intent)
        }
    }

    private val selectedIndex: Int by lazy {
        intent.getIntExtra(EXTRA_SELECTED_INDEX, 0).coerceIn(0, 1)
    }
    private val resumePageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (mRealVM.tableOnlineResumeIndex.value != position) {
                mRealVM.tableOnlineResumeIndex.value = position
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObservers()
        dataUpdate()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        binding.vpResumePages.adapter =
            FragmentStateCacheAdapter(supportFragmentManager, lifecycle).apply {
                setFragments(
                    listOf(
                        ResumeInfoFragment.newInstance(),
                        JobPreferencesFragment.newInstance()
                    )
                )
            }
        binding.vpResumePages.offscreenPageLimit = 2
        mRealVM.tableOnlineResumeIndex.value = selectedIndex
        binding.vpResumePages.setCurrentItem(selectedIndex, false)
        updateResumeTabState(selectedIndex)
        binding.llTab1.setOnSingleClickListener {
            mRealVM.tableOnlineResumeIndex.value = 0
        }
        binding.llTab2.setOnSingleClickListener {
            mRealVM.tableOnlineResumeIndex.value = 1
        }
        binding.vpResumePages.registerOnPageChangeCallback(resumePageChangeCallback)

        binding.llBt.setOnSingleClickListener {
            MyResumeActivity.start(this)
        }

    }

    private fun initObservers() {
        mRealVM.tableOnlineResumeIndex.observe(this) { index ->
            val targetIndex = index.coerceIn(0, 1)
            if (binding.vpResumePages.currentItem != targetIndex) {
                binding.vpResumePages.setCurrentItem(targetIndex, false)
            }
            updateResumeTabState(targetIndex)
        }
        mRealVM.underStandingScore.observe(this) { data ->
            val score = data?.understandingScore ?: 0
            binding.waveView.progress = if (score == 0) 10 else score
            binding.waveView.text = "${score}%"
        }

    }

    private fun dataUpdate() {
        mRealVM.understandingScore()
    }

    private fun updateResumeTabState(index: Int) {
        if (index == 0) {
            binding.layoutTable.setBackgroundResource(R.drawable.ic_understands_table1)
            binding.tvTable1.setTypeface( Typeface.DEFAULT_BOLD)
            binding.tvTable2.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
            binding.ivTable1.visibility = View.VISIBLE
            binding.ivTable2.visibility = View.GONE
            binding.llBt.visibility =  View.VISIBLE

        } else {
            binding.layoutTable.setBackgroundResource(R.drawable.ic_understands_table2)
            binding.tvTable1.setTypeface( Typeface.create("sans-serif-medium", Typeface.NORMAL))
            binding.tvTable2.setTypeface(Typeface.DEFAULT_BOLD)
            binding.ivTable1.visibility = View.GONE
            binding.ivTable2.visibility = View.VISIBLE
            binding.llBt.visibility =  View.GONE
        }
    }

    override fun onDestroy() {
        binding.vpResumePages.unregisterOnPageChangeCallback(resumePageChangeCallback)
        super.onDestroy()
    }

}
