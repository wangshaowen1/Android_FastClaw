package com.kgmedia.fastfilm.base.viewmodel

import com.kgmedia.fastfilm.BR
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.ContentBody
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.viewmodel.BaseRepositoryViewModel

abstract class AppRepositoryViewModel<T : AppRepository> : BaseRepositoryViewModel<T>(){
    override fun id(): Int = BR.layout

 }