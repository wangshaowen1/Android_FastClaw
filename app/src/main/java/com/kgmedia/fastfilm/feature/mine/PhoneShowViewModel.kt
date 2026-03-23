package com.kgmedia.fastfilm.feature.mine

import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.User
import com.nhsd.basic.ext.getResString


class PhoneShowViewModel : AppRepositoryViewModel<AppRepository>() {
    var toolbar = ToolbarViewModel(navigationAction = { back() }, title = R.string.phone_number.getResString())
    val phone = User.get().phoneNum


}