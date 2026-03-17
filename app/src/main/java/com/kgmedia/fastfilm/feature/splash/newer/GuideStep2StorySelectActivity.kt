package com.kgmedia.fastfilm.feature.splash.newer

import android.os.Bundle
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.setGalleryEffect
import com.kgmedia.fastfilm.databinding.ActivityGuideStep2StorySelectBinding
import com.kgmedia.fastfilm.feature.recharge.ItemRechargePowerVM
import com.kgmedia.fastfilm.feature.splash.SplashViewModel
import com.nhsd.basic.BaseViewModelActivity


/**
 * 新手创作流程 第2步 选故事
 */
class GuideStep2StorySelectActivity : BaseViewModelActivity<ActivityGuideStep2StorySelectBinding, SplashViewModel>() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.banner.setGalleryEffect(20, 20, 0.9f, 0.5f)

        binding.banner.adapter = QuickAdapter<ItemRechargePowerVM>(R.layout.item_guide_step2_vp, emptyList())
    }
}
