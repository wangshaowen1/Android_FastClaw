package com.kgmedia.fastfilm.feature.creator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.databinding.ActivityCreationRootBinding
import com.kgmedia.fastfilm.feature.creator.continuewriting.ContinueWritingListFragment
import com.nhsd.basic.BaseViewModelActivity

/**
 * @Introduce:创作根界面
 */
class CreationRootActivity :
    BaseViewModelActivity<ActivityCreationRootBinding, CreationRootViewModel>() {

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            if (context == null) return
            val starter = Intent(context, CreationRootActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventManager.onAppTrack(AppEvent.JG_VIEW_WRITE_STORY_PAGE)
        binding.viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> CreationFragment.newInstance()
                    1 -> CreationFragment.newInstance(CreationFragment.MODEL_CUSTOM)
                    else -> ContinueWritingListFragment.newInstance()
                }
            }

            override fun getCount(): Int {
                return 3
            }
        }
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                mRealVM.tabIndicatorPosition.set(position + positionOffset)
            }

            override fun onPageSelected(position: Int) {
                mRealVM.selectedTab.value = position
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        binding.rbLeft.setOnClickListener {
            binding.viewPager.currentItem = 0
        }
        binding.rbCenter.setOnClickListener {
            EventManager.onAppTrack(AppEvent.JG_CLICK_WRITE_OWN_STORY_BUTTON)
            binding.viewPager.currentItem = 1
        }
        binding.rbRight.setOnClickListener {
            EventManager.onAppTrack(AppEvent.JG_CLICK_CONTINUE_STORY_BUTTON)
            binding.viewPager.currentItem = 2
        }
    }
}