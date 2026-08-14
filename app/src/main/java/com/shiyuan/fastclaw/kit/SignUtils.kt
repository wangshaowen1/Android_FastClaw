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
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.AppEventParam
import com.shiyuan.fastclaw.core.manager.EventManager
import com.tulele.im.utils.AccountHelper

object SignUtils {

    @JvmStatic
    fun signOut() {
        App.instance.deleteAlias()
        // 业务登出时，必须在清空用户凭据之前调用
        AccountHelper.logout()
        User.get().loginOut()
        Cache.get().clear()
        FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.SIGN_OUT))
        GrowingAutotracker.get().cleanLoginUserId()
    }

    @JvmStatic
    fun signIn() {
        AccountHelper.login()

        User.Companion.get().userId?.let { App.instance.setAlias(it) }
        FlashLoginManager.finishAuthActivity()
        ActivityUtils.finishActivity(LoginActivity::class.java)
        ActivityUtils.finishActivity(PhoneLoginDialogActivity::class.java)
        FlowEventBus.postSync(Events.UserChangedEvent(Events.UserChangedEvent.SIGN_IN))
        val mutableMap = mutableMapOf(
            AppEventParam.KEY_PIPE_SOURCE to AppEventParam.VALUE_PIPE_SOURCE_APP,
        ).filterValues { !it.isNullOrBlank() }

        EventManager.onAppTrack(AppEvent.KLX_C_LOGIN_SUCCESS, mutableMap)
    }
}
