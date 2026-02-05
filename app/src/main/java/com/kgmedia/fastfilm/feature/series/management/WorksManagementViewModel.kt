package com.kgmedia.fastfilm.feature.series.management

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.PagedProxy
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.request.DeleteWorkBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.series.EpisodePagingSource
import com.kgmedia.fastfilm.feature.series.SeriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2026/2/5/周四  20:19
 * @Version: 1.0
 * *****************************************
 */
class WorksManagementViewModel : AppRepositoryViewModel<SeriesRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "管理作品",
        background = Color.TRANSPARENT,
    )


     val workId by lazy { mBundle.getString(WorksManagementActivity.WORK_ID) }

    //删除完成
     val deleteCompleted = MutableLiveData(false)


    fun episodeList(): Flow<PagingData<EpisodeInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = PagedProxy.DEFAULT_PAGE_SIZE * 3,
                pageSize = PagedProxy.DEFAULT_PAGE_SIZE
            ),
            pagingSourceFactory = {
                EpisodePagingSource(workId)
            }
        ).flow.cachedIn(viewModelScope)
    }


    /**
     * 删除作品
     */
    fun delete(workIds: List<String>) {
        if (workIds.isEmpty()) return
        launchWithLoading {
            response(
                bean = mRepo.api.batchDeleteWorkEpisode(DeleteWorkBody(ids = workIds)),
                result = {
                    deleteCompleted.value = true
                })
        }
    }
}