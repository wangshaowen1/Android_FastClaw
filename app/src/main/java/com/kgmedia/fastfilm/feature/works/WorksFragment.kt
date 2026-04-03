package com.kgmedia.fastfilm.feature.works

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.ActivityUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.adapter.FragmentStateCacheAdapter
import com.kgmedia.fastfilm.common.ext.removeTheRipples
import com.kgmedia.fastfilm.common.ext.setScale
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.databinding.loadGifUrl
import com.kgmedia.fastfilm.core.network.apis.response.ActivityConfigData
import com.kgmedia.fastfilm.core.network.apis.response.FloatWindowInfo
import com.kgmedia.fastfilm.core.network.apis.response.PopupInfo
import com.kgmedia.fastfilm.databinding.FragmentWorksBinding
import com.kgmedia.fastfilm.feature.main.ActivityDialog
import com.kgmedia.fastfilm.feature.main.MainActivity
import com.kgmedia.fastfilm.feature.main.MainViewModel
import com.kgmedia.fastfilm.feature.works.assetmanager.AssetFolderFragment
import com.kgmedia.fastfilm.router.AppRouter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.titles.ScaleTransitionPagerTitleView

class WorksFragment : BaseViewModelFragment<FragmentWorksBinding, WorksViewModel>() {


    companion object {
        const val TAG = "WorksFragment"

        @JvmStatic
        fun start() {
            FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_MY_WORKS))
            ActivityUtils.finishToActivity(MainActivity::class.java, false)
        }
    }

    private val parentVm by lazy {
        getViewModelOfActivity<MainViewModel>()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initViewpager()
        dataUpdate()
    }

    private fun initViewpager() {
        val titles = listOf(
            getString(R.string.my_works),
            getString(R.string.asset_management)
        )
        val navigator = CommonNavigator(context)
        navigator.isAdjustMode = false
        navigator.isReselectWhenLayout = false
        navigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount(): Int {
                return titles.size
            }

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                val titleView = ColorTransitionPagerTitleView(context)
                titleView.setIsSelectedBold(true)
                titleView.setPadding(0, 0, 20.dp, 0)
                titleView.setLines(1)
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                titleView.gravity = Gravity.CENTER

                titleView.text = titles.getOrNull(index)
                titleView.normalColor = topActivity?.getThemeColor(R.attr.textColor4) ?: 0
                titleView.selectedColor = topActivity?.getThemeColor(R.attr.textColor1) ?: 0
                titleView.setOnClickListener {
                    binding.viewPager2.currentItem = index
                }
                return titleView
            }

            override fun getIndicator(context: Context): IPagerIndicator? {
                    return null
            }
        }
        binding.magicIndicator.navigator = navigator

        val fragmentList = arrayListOf<Fragment>()
        fragmentList.add(MyWorksFragment())
        fragmentList.add(AssetFolderFragment())

        val viewPagerAdapter = FragmentStateCacheAdapter(
            childFragmentManager,
            lifecycle
        )
        viewPagerAdapter.setFragments(fragmentList)
        binding.viewPager2.adapter = viewPagerAdapter
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager2)
        binding.viewPager2.setCurrentItem(0, false)
        binding.viewPager2.removeTheRipples()
    }


    private fun dataUpdate() {
        parentVm?.activityConfigCache?.observe(this) {
            handlePopup(it.getPopupInfo(ActivityConfigData.TAB_MY_WORKS))
            handleFloatWindow(it.getFloatWindowInfo(ActivityConfigData.TAB_MY_WORKS))
        }
    }


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnEdit -> {
                mRealVM.editModel.value = true
            }
        }
    }

    private fun handlePopup(popup: PopupInfo?) {
        popup?.let {
            if (popup.isShow()) {
                ActivityDialog.show(popup)
                Config.get().activityPopupShowMark(popup.id)
            }
        }
    }

    private fun handleFloatWindow(float: FloatWindowInfo?) {
        if (float != null && float.isShow()) {
            binding.floatingBall.isVisible = true
            loadGifUrl(
                binding.ivFloating,
                float.imageUrl,
                R.drawable.img_default_cover_1_1.getResDrawable()
            )
            Config.get().activityFloatShowMark(float.id)
            binding.floatingBall.setOnMfClickListener {
                AppRouter.jump(float.jumpUrl)
            }
        } else {
            binding.floatingBall.visibility = View.GONE
        }
    }
}