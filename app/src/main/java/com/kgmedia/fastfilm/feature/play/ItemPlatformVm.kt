package com.kgmedia.fastfilm.feature.play

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.network.apis.entity.PlatformInfo
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity


class ItemPlatformVm(
    data: PlatformInfo?
) : AppItemViewModel<PlatformInfo>(data) {
    fun onPlatformClick() {
        SimpleBrowserActivity.launch(topActivity, data.get()?.h5Url)
    }
}