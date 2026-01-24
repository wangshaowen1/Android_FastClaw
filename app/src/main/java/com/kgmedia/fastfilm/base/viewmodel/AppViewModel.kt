package com.kgmedia.fastfilm.base.viewmodel

import com.nhsd.basic.viewmodel.BaseLayoutViewModel
import com.nhsd.basic.viewmodel.VariableId
import com.kgmedia.fastfilm.BR


open class AppViewModel : BaseLayoutViewModel(), VariableId {

    override fun id(): Int = BR.layout

}