package com.kgmedia.fastfilm.feature.play

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.PlatformInfo
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kgmedia.fastfilm.router.AppRouter


class ItemPlatformVm(
    data: PlatformInfo?
) : AppItemViewModel<PlatformInfo>(data) {
    fun onPlatformClick() {
        EventManager.onAppTrack(AppEvent.SF_CLICK_WORK_VIDEO_BUTTON)
        AppRouter.jump(data.get()?.h5Url)
    }
}