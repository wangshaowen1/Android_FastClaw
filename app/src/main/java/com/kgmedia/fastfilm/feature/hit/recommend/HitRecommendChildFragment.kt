package com.kgmedia.fastfilm.feature.hit.recommend

import android.os.Bundle
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.network.apis.entity.HitTabInfo
import com.kgmedia.fastfilm.databinding.FragmentHitRecommendChildBinding
import com.nhsd.basic.BaseViewModelFragment

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
}



