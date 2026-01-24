package com.kgmedia.fastfilm.feature.play

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.core.ext.onUserStatusChanged
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.databinding.FragmentRecommendShortPlayPagerBinding
import com.kgmedia.fastfilm.kit.PlaylistListener
import com.kgmedia.fastfilm.kit.setupMediaSourceManager
import com.kgmedia.fastfilm.kit.source.MediaSourceManager
import com.kgmedia.fastfilm.kit.source.impl.RecommendShortsPageSource
import com.kgmedia.fastfilm.kit.source.impl.ReleasedShortDetailSource
import com.kgmedia.fastfilm.widget.NeedUpdateFragmentStateAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.launch

/**
 * 短剧播放
 */
class RecommendShortPlayPagerFragment :
    BaseViewModelFragment<FragmentRecommendShortPlayPagerBinding, ShortDramaPlayPagerViewModel>(),
    PlaylistListener {
    private var isHide: Boolean = false
    private val adapter by lazy {
        MyFragmentStateAdapter().apply {
            mRealVM.playlist?.let {
                submitData(it)
            }
        }
    }
    var manager: MediaSourceManager? = null
    override fun onViewModelCreated() {
        super.onViewModelCreated()
        mRealVM.shortsPlayer.setupMediaSourceManager()
            .setMediaSource(RecommendShortsPageSource()).setPlayableSource(
                ReleasedShortDetailSource()
            ).apply {
                manager = this
                lifecycleScope.launch {
                    newPageDataFlow.collect { added ->
                        if (mRealVM.shortsPlayer.playlist.isNullOrEmpty()) {
                            binding.loadingView.showEmpty()
                        } else {
                            binding.loadingView.showContent()
                        }
                        mRealVM.playlist?.let { it1 -> setAdapterList(it1) }
                    }
                }

                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        loadErrorFlow.collect {
                            if (mRealVM.playlist.isNullOrEmpty()) {
                                binding.loadingView.showRetry(it)
                            }
                        }
                    }
                }
            }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.loadingView.binding.icon.alpha = 0.5f
        binding.loadingView.binding.errorPromptView.setTextColor(R.color.color_8f8f8f.getResColor())
        binding.loadingView.binding.noDataIcon.alpha = 0.5f
        if (mRealVM.playlist?.isEmpty() == true) {
            binding.loadingView.showLoading()
        } else {
            binding.loadingView.showContent()
        }
        binding.loadingView.setRetryListener {
            binding.loadingView.showLoading()
            manager?.retry()
        }
        binding.viewPager2.orientation = ViewPager2.ORIENTATION_VERTICAL
        Logger.i("onCreate", "adapter size=${adapter.itemCount}")
        binding.viewPager2.adapter = adapter
        binding.viewPager2.registerOnPageChangeCallback(callback)
        mRealVM.checkedShort.observe(this) {
            val index = mRealVM.playlist?.indexOfFirst { item -> item.dramasId == it?.dramasId }
            if (index != null && index >= 0 && binding.viewPager2.currentItem != index) {
                binding.viewPager2.setCurrentItem(index, false)
            }
        }
        observeEvents {
            onUserStatusChanged {
                manager?.refresh()
            }
        }
    }

    private fun setAdapterList(data: List<ShortDramaInfo>) {
        if (adapter.itemCount == 0 && visible() && data.getOrNull(0)?.isLocked() == false) {
            mRealVM.shortsPlayer.play()
        }
        adapter.submitData(data)
    }

    val callback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if ((mRealVM.playlist?.size ?: 0) > 0) {
                adapter.onPageSelected(position)
                val realPosition = adapter.realPosition(position)
                val skipTo = mRealVM.playlist?.getOrNull(realPosition)
                if (mRealVM.shortsPlayer.currentShort() != skipTo) {
                    mRealVM.shortsPlayer.skipTo(realPosition)
                    if (skipTo?.unlockInfo?.isLocked() == true) {
                        mRealVM.shortsPlayer.pause()
                    }else {
                        mRealVM.shortsPlayer.play()
                    }
                }
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        onHideChange(hidden)
    }

    private fun onHideChange(hidden: Boolean) {
        userVisibleHint = !hidden
    }

    @Deprecated("Deprecated in Java")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        this.isHide = !isVisibleToUser
        Logger.i("index=2 home track onVisibleToUser:$isVisibleToUser")
        if (isVisibleToUser) {
            mRealVM.shortsPlayer.addListener(this)
            if (mRealVM.shortsPlayer.currentShort()?.isLocked() == false) {
                mRealVM.shortsPlayer.play()
            }

        } else {
            mRealVM.shortsPlayer.pause()
        }
    }

    private fun visible() = !isHide && isVisible
    inner class MyFragmentStateAdapter :
        NeedUpdateFragmentStateAdapter<ShortDramaInfo?>(
            childFragmentManager,
            lifecycle,
            false
        ) {

        override fun createFragment(position: Int, item: ShortDramaInfo?): Fragment {
            return ShortDramaPlayerFragment.newInstance(
                item?.dramasId, false
            )
        }

        override fun isDiff(old: ShortDramaInfo?, new: ShortDramaInfo?): Boolean {
            return old?.dramasId != new?.dramasId
        }
    }

}