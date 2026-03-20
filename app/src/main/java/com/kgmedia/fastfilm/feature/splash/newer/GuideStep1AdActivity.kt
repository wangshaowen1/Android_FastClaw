package com.kgmedia.fastfilm.feature.splash.newer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.ActivityGuideStep1AdBinding
import com.kgmedia.fastfilm.feature.splash.SplashViewModel
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener


/**
 * 新手创作流程 第一步
 */
class GuideStep1AdActivity : BaseViewModelActivity<ActivityGuideStep1AdBinding, SplashViewModel>() {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, GuideStep1AdActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventManager.onAppTrack(AppEvent.SF_VIEW_BEGINNER_CREATION_GUIDE_PAGE)
        binding.btnNext.setOnSingleClickListener {
            EventManager.onAppTrack(AppEvent.SF_CLICK_BEGINNER_CREATION_GUIDE_PAGE_BUTTON)
            GuideStep2StorySelectActivity.start(this)
            finish()
        }
    }
}
