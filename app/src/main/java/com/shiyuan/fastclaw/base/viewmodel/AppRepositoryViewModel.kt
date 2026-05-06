package com.shiyuan.fastclaw.base.viewmodel

import com.shiyuan.fastclaw.BR
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.ContentBody
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.viewmodel.BaseRepositoryViewModel

abstract class AppRepositoryViewModel<T : AppRepository> : BaseRepositoryViewModel<T>(){
    override fun id(): Int = BR.layout

 }