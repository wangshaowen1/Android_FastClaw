package com.shiyuan.fastclaw.feature.settings.phone

import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.User

class PhoneShowViewModel : AppRepositoryViewModel<AppRepository>() {
    var toolbar = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.phone_number.getResString()
    )
    val phone = User.Companion.get().phoneNum


}
