package com.kgmedia.fastfilm.feature.works

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.db.entity.WorkInformationInfo
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import kotlinx.coroutines.flow.Flow

class MyWorksViewModel : AppRepositoryViewModel<AppRepository>() {

    //当前显示状态是查看(true)还是编辑(false)
    var isView = MutableLiveData(true)

    //当前选中的数量
    var selectedCount = MutableLiveData(0)


    /**
     * 投诉消息列表
     */
    @OptIn(ExperimentalPagingApi::class)
    fun getWorkInformation(): Flow<PagingData<WorkInformationInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = 200,
            ), remoteMediator = WorkInformationRemoteMediator(
                database = mRepo.appDatabase
            )
        ) {
            mRepo.appDatabase.workInformationDao().getPosts()
        }.flow.cachedIn(viewModelScope)
    }

    fun delete(workIds: List<String>) {
        if (workIds.isEmpty()) return
        launchWithLoading {
            //删除数据库中的
            mRepo.appDatabase.workInformationDao().deleteByWorkIds(workIds)
        }
    }
}