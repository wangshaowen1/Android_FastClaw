package com.shiyuan.fastclaw.feature.splash

import androidx.lifecycle.MutableLiveData
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository

/**
 * @Introduce: 启动页 ViewModel，管理启动流程
 */
class SplashViewModel : AppRepositoryViewModel<AppRepository>() {

    sealed class LaunchDestination {
        data class Home(val deepLink: String?) : LaunchDestination()
    }

    val launchDestination = MutableLiveData<LaunchDestination>()

    val hasAgreed: Boolean
        get() = com.shiyuan.fastclaw.App.instance.hasAgree()

    fun checkPolicyAndLaunch() {
        if (!hasAgreed) return
        proceedToNext()
    }

    fun onPolicyAgreed() {
        proceedToNext()
    }

    private fun proceedToNext() {
        // 不管登没登录，都跳转到首页
        launchDestination.value = LaunchDestination.Home(null)
    }
}
