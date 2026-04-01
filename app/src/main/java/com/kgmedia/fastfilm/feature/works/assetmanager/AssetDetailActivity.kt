package com.kgmedia.fastfilm.feature.works.assetmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.ScrollingMovementMethod
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.adapter.FragmentStateCacheAdapter
import com.kgmedia.fastfilm.common.ext.removeTheRipples
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.CommonNavigatorUtils
import com.kgmedia.fastfilm.core.network.apis.entity.TabEntity
import com.kgmedia.fastfilm.databinding.ActivityAssetDtlBinding
import com.kgmedia.fastfilm.databinding.ActivityAssetsBinding
import com.kgmedia.fastfilm.feature.works.MyWorksFragment
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.CommonNavigator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import com.nhsd.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import com.nhsd.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView

/**
 * 资产详情页 可修改资产描述 重新生成图片
 */
class AssetDetailActivity : BaseViewModelActivity<ActivityAssetDtlBinding, AssetDetailViewModel>() {


    companion object {
        const val TAG = "AssetManagerActivity"

        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, AssetDetailActivity::class.java)
            context.startActivity(starter)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.desc.movementMethod = ScrollingMovementMethod()
    }



}