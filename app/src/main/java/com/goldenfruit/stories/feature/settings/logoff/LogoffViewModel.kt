package com.kgmedia.fastfilm.feature.settings.logoff

import android.graphics.Color
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: 小元音乐圈
 * @Author: SZYMN
 * @Introduce:注销账号页面
 * @Data: 2025/4/22  13:55
 * @Version: 1.0
 * *****************************************
 */
class LogoffViewModel : AppRepositoryViewModel<AppRepository>() {
    var mToolbarVM = ToolbarViewModel(navigationAction = { back() }, title = "注销账号", background = Color.TRANSPARENT)
    val checkedPolicy by lazy { ObservableField(false) }
    val removeAccountState by lazy { ObservableInt(-1) }

    var firstRequest = true
    override fun onStart() {
        super.onStart()
        launchWithLoading(firstRequest) {
            response(mRepo.api.logOffStatus(UserTokenBody())) {
                data?.removeAccountState?.let { removeAccountState.set(it) }
                firstRequest = false
            }
        }
    }

    fun checkPolicy() {
        checkedPolicy.set(checkedPolicy.get() != true)
    }

    fun logOff() {
        startActivity(ApplyLogOffActivity::class.java)
//        removeAccountState.set(-1)
    }

    /**
     * 取消注销
     */
    fun cancelLogoff() {
        launchWithLoading {
            response(mRepo.api.cancelLogOff(UserTokenBody())) {
                "取消成功".showToast()
                finish()
            }
        }
    }
}