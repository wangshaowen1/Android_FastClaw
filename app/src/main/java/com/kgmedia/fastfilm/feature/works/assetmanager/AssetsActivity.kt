package com.kgmedia.fastfilm.feature.works.assetmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.FragmentStateCacheAdapter
import com.kgmedia.fastfilm.common.ext.removeTheRipples
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.CommonNavigatorUtils
import com.kgmedia.fastfilm.core.network.apis.entity.TabEntity
import com.kgmedia.fastfilm.core.network.apis.request.QueryAssetBody
import com.kgmedia.fastfilm.databinding.ActivityAssetsBinding
import com.kgmedia.fastfilm.feature.works.assetmanager.child.AssetsChildFragment
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView

/**
 * 资产管理
 */
class AssetsActivity : BaseViewModelActivity<ActivityAssetsBinding, AssetsViewModel>() {


    companion object {
        const val TAG = "AssetsActivity"

        @JvmStatic
        fun start(context: Context?, workId: String?, workTitle: String?) {
            if (context == null) return
            val starter = Intent(context, AssetsActivity::class.java)
            starter.putExtra(Constants.Extra.WORK_ID, workId)
            starter.putExtra(Constants.Extra.TITLE, workTitle)
            context.startActivity(starter)
        }
    }

    // 模拟数据源
    private val mDataList by lazy {
        mutableListOf(
            TabEntity(getString(R.string.roles), 0),
            TabEntity(getString(R.string.scenes), 0),
            TabEntity(getString(R.string.props), 0)
        )
    }

    private lateinit var navigator: CommonNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    fun dataUpdate() {
        mRealVM.roleCount.observe(this) {
            updateBadgeCount(0, it)
        }
        mRealVM.sceneCount.observe(this) {
            updateBadgeCount(1, it)
        }
        mRealVM.propCount.observe(this) {
            updateBadgeCount(2, it)
        }
    }


    private fun initView() {
        navigator = CommonNavigator(this)
        navigator.isAdjustMode = true
        navigator.isReselectWhenLayout = false
        navigator.adapter = object : CommonNavigatorAdapter() {
            override fun getCount(): Int {
                return mDataList.size
            }

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                val titleView = ColorTransitionPagerTitleView(context)
                titleView.setLines(1)
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                titleView.gravity = Gravity.CENTER

                titleView.text = buildBadgeSpannable(mDataList[index])
                titleView.normalColor = topActivity?.getThemeColor(R.attr.textColor4) ?: 0
                titleView.selectedColor = topActivity?.getThemeColor(R.attr.textColor1) ?: 0
                titleView.setOnClickListener {
                    binding.viewPager2.currentItem = index
                }
                return titleView
            }

            override fun getIndicator(context: Context): IPagerIndicator {
                return CommonNavigatorUtils.getLinePagerIndicator(this@AssetsActivity, 11f).also {
                    it.setColors(topActivity?.getThemeColor(R.attr.textColor1))
                }
            }
        }

        binding.magicIndicator.navigator = navigator


        val fragmentList = arrayListOf<Fragment>()
        fragmentList.add(AssetsChildFragment.newInstance(QueryAssetBody.BIZ_TYPE_ROLE, mRealVM.workId))
        fragmentList.add(AssetsChildFragment.newInstance(QueryAssetBody.BIZ_TYPE_SCENE, mRealVM.workId))
        fragmentList.add(AssetsChildFragment.newInstance(QueryAssetBody.BIZ_TYPE_PROP, mRealVM.workId))

        val viewPagerAdapter = FragmentStateCacheAdapter(
            supportFragmentManager,
            lifecycle
        )
        viewPagerAdapter.setFragments(fragmentList)
        binding.viewPager2.adapter = viewPagerAdapter
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager2)
        binding.viewPager2.setCurrentItem(0, false)
        binding.viewPager2.removeTheRipples()
    }


    /**
     * 构建带有角标的 SpannableString
     */
    private fun buildBadgeSpannable(item: TabEntity): SpannableString {
        // 拼接字符串，例如 "角色5"（如果想要一点间距，可以改成 "${item.title} ${item.badgeCount}"）
        val badgeStr = if (item.badgeCount > 0) item.badgeCount.toString() else ""
        val rawText = "${item.title}$badgeStr"

        val spannable = SpannableString(rawText)

        if (badgeStr.isNotEmpty()) {
            val start = item.title.length
            val end = rawText.length

            // 关键点 1：设置角标字号缩小。
            // 因为 TextView 默认是基线(Baseline)对齐，缩小字号后，数字自然就靠在右下角了。
            spannable.setSpan(
                RelativeSizeSpan(0.857f), // 字号缩小为原来的 0.65 倍，可根据 UI 需求微调
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // 关键点 2：设置角标独立颜色。
            // ForegroundColorSpan 的优先级高于 TextView 的 setTextColor。
            // 这样主标题在滑动时变色（灰 <-> 白），角标会始终保持这里设置的暗灰色。
//            spannable.setSpan(
//                ForegroundColorSpan(Color.parseColor("#666666")),
//                start,
//                end,
//                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
        }
        return spannable
    }

    /**
     * 动态更新角标的方法（对外暴露调用）
     * @param index 要更新的 Tab 索引
     * @param newCount 新的数量
     */
    fun updateBadgeCount(index: Int, newCount: Int) {
        if (index < 0 || index >= mDataList.size) return

        // 1. 更新数据源
        mDataList[index].badgeCount = newCount

        // 2. 局部刷新：直接找到对应的 View 更新文字，性能最优
        val titleView = navigator.getPagerTitleView(index) as? ColorTransitionPagerTitleView
        titleView?.text = buildBadgeSpannable(mDataList[index])
    }
}