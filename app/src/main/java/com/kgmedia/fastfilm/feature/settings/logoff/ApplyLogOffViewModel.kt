package com.kgmedia.fastfilm.feature.settings.logoff

import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response
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