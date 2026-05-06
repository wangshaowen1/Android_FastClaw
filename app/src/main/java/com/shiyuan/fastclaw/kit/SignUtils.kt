package com.shiyuan.fastclaw.kit

import com.blankj.utilcode.util.ActivityUtils
import com.growingio.android.sdk.autotrack.GrowingAutotracker
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.common.Cache
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.manager.FlashLoginManager
import com.shiyuan.fastclaw.feature.login.LoginActivity
import com.shiyuan.fastclaw.feature.login.flash.PhoneLoginDialogActivity
import com.nhsd.basic.bus.FlowEventBus

object SignUtils {

    @JvmStatic
    fun signOut() {
        App.instance.deleteAlias()
        User.get().loginOut()
        Cache.get().clear()
        FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.SIGN_OUT))
        GrowingAutotracker.get().cleanLoginUserId()
    }

    @JvmStatic
    fun signIn() {
        User.Companion.get().userId?.let { App.instance.setAlias(it) }
        FlashLoginManager.finishAuthActivity()
        ActivityUtils.finishActivity(LoginActivity::class.java)
        ActivityUtils.finishActivity(PhoneLoginDialogActivity::class.java)
        FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.SIGN_IN))
    }
}