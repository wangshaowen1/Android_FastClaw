package com.shiyuan.fastclaw.base.viewmodel

import com.nhsd.basic.viewmodel.BaseLayoutViewModel
import com.nhsd.basic.viewmodel.VariableId
import com.shiyuan.fastclaw.BR


open class AppViewModel : BaseLayoutViewModel(), VariableId {

    override fun id(): Int = BR.layout

}