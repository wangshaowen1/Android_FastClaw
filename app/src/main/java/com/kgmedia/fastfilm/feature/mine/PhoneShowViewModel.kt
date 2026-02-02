package com.kgmedia.fastfilm.feature.mine

import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.User


class PhoneShowViewModel : AppRepositoryViewModel<AppRepository>() {
    var toolbar = ToolbarViewModel(navigationAction = { back() }, title = "手机号码")
    val phone = User.get().phoneNum


}