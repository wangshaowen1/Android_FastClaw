package com.kgmedia.fastfilm.feature.home

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.HitRecommendInfo
import com.kgmedia.fastfilm.feature.play.DramasPlayPagerActivity

class ItemHomeDramaVm(data: HitRecommendInfo?) : AppItemViewModel<HitRecommendInfo>(data) {

    fun onClick() {
        DramasPlayPagerActivity.start(null)
    }
}