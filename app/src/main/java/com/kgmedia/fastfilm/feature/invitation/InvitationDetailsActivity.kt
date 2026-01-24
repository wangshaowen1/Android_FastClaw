package com.kgmedia.fastfilm.feature.invitation

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.kgmedia.fastfilm.common.adapter.FragmentStateCacheAdapter
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.CommonNavigatorUtils
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.databinding.ActivityInvitationDetailsBinding
import com.kgmedia.fastfilm.feature.invitation.income.InvitationIncomeFragment
import com.kgmedia.fastfilm.feature.invitation.person.InviteeListFragment
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请明细
 * @Data: 2025/10/13/周一  10:14
 * @Version: 1.0
 * *****************************************
 */
class InvitationDetailsActivity :
    BaseViewModelActivity<ActivityInvitationDetailsBinding, InvitationDetailsViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            val activity = activity ?: topActivity ?: return
            val intent = Intent(activity, InvitationDetailsActivity::class.java)
            activity.startActivity(intent)
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
                "邀请收益",
                "邀请人"
            ),
            binding.viewPager,
            indicatorView = CommonNavigatorUtils.getLinePagerIndicatorGradient(context = this)
                .also { indicator ->
                    indicator?.mode = LinePagerIndicator.PRECISION_MODE_LEFT_ALIGN
                    indicator?.yOffset = (9).dp.toFloat()
                }
        )
        val fragmentList = arrayListOf<Fragment>()
        fragmentList.add(InvitationIncomeFragment.newInstance())
        fragmentList.add(InviteeListFragment.newInstance())

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
        observeEvents {
            on<Events.InvitationIncomeEvent> { event ->
                if (event.amountOfMoney > 0) {
                    mRealVM.mTotalIncome.set(event.amountOfMoney.toLong().formatMoney())
                }
            }
        }
    }

}