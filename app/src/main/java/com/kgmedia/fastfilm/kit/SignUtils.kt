package com.kgmedia.fastfilm.kit

import com.blankj.utilcode.util.ActivityUtils
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.common.Cache
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.manager.FlashLoginManager
import com.kgmedia.fastfilm.feature.aicompose.generating.GeneratingViewModel
import com.kgmedia.fastfilm.feature.login.LoginActivity
import com.kgmedia.fastfilm.feature.login.flash.PhoneLoginDialogActivity
import com.growingio.android.sdk.autotrack.GrowingAutotracker
import com.nhsd.basic.bus.FlowEventBus

object SignUtils {

    @JvmStatic
    fun signOut() {
        GeneratingViewModel.generating = false
        App.instance.deleteAlias()
        User.get().loginOut()
        Cache.get().clear()
        FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.SIGN_OUT))
//        GrowingAutotracker.get().cleanLoginUserId()
    }

    @JvmStatic
    fun signIn() {
        GeneratingViewModel.generating = true
        User.Companion.get().userId?.let { App.instance.setAlias(it) }
        FlashLoginManager.finishAuthActivity()
        ActivityUtils.finishActivity(LoginActivity::class.java)
        ActivityUtils.finishActivity(PhoneLoginDialogActivity::class.java)
        FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.SIGN_IN))
    }
}