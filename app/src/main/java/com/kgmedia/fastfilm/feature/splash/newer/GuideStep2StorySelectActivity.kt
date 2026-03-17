package com.kgmedia.fastfilm.feature.splash.newer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.setGalleryEffect
import com.kgmedia.fastfilm.core.network.apis.entity.ExampleWorkInfo
import com.kgmedia.fastfilm.databinding.ActivityGuideStep2StorySelectBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener


/**
 * 新手创作流程 第2步 选故事
 */
class GuideStep2StorySelectActivity : BaseViewModelActivity<ActivityGuideStep2StorySelectBinding, GuideStep2ViewModel>() {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, GuideStep3GeneratingActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.banner.setGalleryEffect(20, 20, 0.9f, 0.5f)

        mRealVM.examples.observe(this) {
            if (binding.banner.adapter == null && !it.isNullOrEmpty()) {
                binding.banner.adapter = QuickAdapter(R.layout.item_guide_step2_vp, it.map { info ->
                    ItemGuideStep2Vm(info)
                })
            }
        }

        binding.btnConfirm.setOnSingleClickListener {
            val exampleWorkInfo = mRealVM.examples.value?.getOrNull(binding.banner.currentItem)
            GuideStep3GeneratingActivity.start(this, exampleWorkInfo)
            finish()
        }

    }
}
