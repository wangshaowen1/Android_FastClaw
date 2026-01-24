package com.kgmedia.fastfilm.feature.payment.points.details

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.kgmedia.fastfilm.common.adapter.FragmentStateCacheAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.CommonNavigatorUtils
import com.kgmedia.fastfilm.databinding.ActivityPointsDetailBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:余额明细
 * @Data: 2025/9/10/周三  17:49
 * @Version: 1.0
 * *****************************************
 */
class PointsDetailActivity :
    BaseViewModelActivity<ActivityPointsDetailBinding, PointsDetailViewModel>() {

    companion object {


        fun start(activity: Activity?) {
            val activity = activity ?: topActivity
            val intent = Intent(activity, PointsDetailActivity::class.java)
            activity?.startActivity(intent)
        }
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.magicIndicator.navigator = CommonNavigatorUtils.getCommonNavigatorVp2(
            this,
            listOf(
                "充值明细",
                "积分明细"
            ),
            binding.viewPager,
            indicatorView = CommonNavigatorUtils.getLinePagerIndicatorGradient(context = this)
                .also { indicator ->
                    indicator?.lineWidth= (20).dp.toFloat()
                    indicator?.mode = LinePagerIndicator.MODE_EXACTLY
                    indicator?.yOffset = (1).dp.toFloat()
                }
        )
        val fragmentList = arrayListOf<Fragment>()
        fragmentList.add(PointsDetailFragment.newInstance(typeProtagonist=PointsDetailFragment.TYPE_RECHARGE))
        fragmentList.add(PointsDetailFragment.newInstance(typeProtagonist=PointsDetailFragment.TYPE_CONSUME))

        val viewPagerAdapter = FragmentStateCacheAdapter(
            supportFragmentManager,
            lifecycle
        )
        viewPagerAdapter.setFragments(fragmentList)
        binding.viewPager.adapter = viewPagerAdapter
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager)

        binding.viewPager.setCurrentItem(0, false)
    }


    private fun dataUpdate() {

    }
}