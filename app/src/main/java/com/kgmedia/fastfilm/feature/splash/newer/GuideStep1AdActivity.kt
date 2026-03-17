package com.kgmedia.fastfilm.feature.splash.newer

import android.os.Bundle
import com.kgmedia.fastfilm.databinding.ActivityGuideStep1AdBinding
import com.kgmedia.fastfilm.feature.splash.SplashViewModel
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener


/**
 * 新手创作流程 第一步
 */
class GuideStep1AdActivity : BaseViewModelActivity<ActivityGuideStep1AdBinding, SplashViewModel>() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnNext.setOnSingleClickListener {
            GuideStep2StorySelectActivity.start(this)
            finish()
        }
    }
}
