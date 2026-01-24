package com.kgmedia.fastfilm.feature.mine.episode

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:剧集
 * @Data: 2025/11/4/周二  17:19
 * @Version: 1.0
 * *****************************************
 */
class EpisodeViewModel : AppRepositoryViewModel<AppRepository>() {

    //当前选中的是已上架(true)还是未上架(false)
    var isLaunch = ObservableBoolean(true)

    //已上架数量
    var totalCount= MutableLiveData(0)
    //未上架数量
    var totalCountNotLaunch= MutableLiveData(0)

}