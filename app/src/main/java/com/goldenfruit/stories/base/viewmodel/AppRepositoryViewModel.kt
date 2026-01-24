package com.kgmedia.fastfilm.base.viewmodel

import com.nhsd.basic.viewmodel.BaseRepositoryViewModel
import com.kgmedia.fastfilm.BR
import com.kgmedia.fastfilm.core.AppRepository

abstract class AppRepositoryViewModel<T : AppRepository> : BaseRepositoryViewModel<T>(){
    override fun id(): Int = BR.layout
 }