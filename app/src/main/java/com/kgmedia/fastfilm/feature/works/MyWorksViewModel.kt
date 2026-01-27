package com.kgmedia.fastfilm.feature.works

import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.core.AppRepository

class MyWorksViewModel : AppRepositoryViewModel<AppRepository>() {
    val adapter by lazy { QuickAdapter(R.layout.item_my_work, List(20) { ItemMyWorkVM() }) }
}