package com.kgmedia.fastfilm.feature.hit.recommend

import android.os.Bundle
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.startRefreshWithAdapter
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.core.network.apis.entity.HitTabInfo
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateBody
import com.kgmedia.fastfilm.databinding.FragmentHitRecommendChildBinding
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.payment.vip.MembershipBenefitsActivity
import com.kgmedia.fastfilm.router.AppNavigator
import com.kgmedia.fastfilm.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.launch

class HitRecommendChildFragment : BaseViewModelFragment<FragmentHitRecommendChildBinding, HitRecommendChildViewModel>() {

    companion object {
        fun newInstance(tab: HitTabInfo?): HitRecommendChildFragment {
            val fragment = HitRecommendChildFragment()
            fragment.arguments = tab?.let {
                Bundle().apply {
                    putParcelable(Constants.Extra.TAB, it)
                }
            }
            return fragment
        }
    }

    val hitRecommendAdapter by lazy {
        HitRecommendAdapter().apply {
            onAdapterItemClick = {
                if (!AppNavigator.checkLoginIntercepted(true)) {
//                    if (it?.isVipValid() == true && !User.get().isVip) {
//                        MembershipBenefitsActivity.start(activity, MembershipBenefitsActivity.Scene.MakeSame)
//                    } else {
                        EventManager.onAppTrack(AppEvent.JG_CLICK_FADDISH_PAGE_CELL, mapOf(GrowingIO.KEY_ALL_GENERAL_ID to it?.popularRecommendDramasId))
                        EventManager.hitRecommendClickReport(it?.popularRecommendDramasId)
                        StoryEditorActivity.start(
                            mActivity,
                            null,
                            it?.styleId,
                            it?.videoRatio,
                            DramaGenerateBody.GEN_WAY_MAKE_SAME_STORY,
                            it?.popularRecommendDramasId
                        )
//                    }
                }
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.recyclerView.apply {
            isFocusable = false
            adapter = hitRecommendAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = hitRecommendAdapter,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    displayedHeight = 400.dp,
                    refresh = {
                        hitRecommendAdapter.refresh()
                    },
                    requestButtonTextAndImg = false,
                )
            )
            doOnLayout {
                scrollToPosition(0)
            }
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = hitRecommendAdapter
        )
        binding.lsv.apply {
            makeScrollable()
            setRetryListener {
                //刷新
                hitRecommendAdapter.refresh()
            }
        }
    }

    fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.dataGet.collect {
                hitRecommendAdapter.submitData(it)
            }
        }
    }
}