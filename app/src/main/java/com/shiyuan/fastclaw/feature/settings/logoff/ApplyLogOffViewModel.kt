package com.shiyuan.fastclaw.feature.settings.logoff

import androidx.lifecycle.MutableLiveData
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.response
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast


/**
 * 注销账号-提交
 */
class ApplyLogOffViewModel : AppRepositoryViewModel<AppRepository>() {
    var toolbar = ToolbarViewModel(navigationAction = { back() }, title = R.string.app_cancel_the_account.getResString())
    val content by lazy { MutableLiveData<String>() }

    /**
     * 提交注销申请
     */
    fun commitLogoff(content: String) {
        launchWithLoading {
            response(mRepo.api.logOff(UserTokenBody())) {
                R.string.account_deletion_notice_period.getResString().showToast()
                finish()
            }
        }
    }
}