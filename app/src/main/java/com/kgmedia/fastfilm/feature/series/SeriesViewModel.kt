package com.kgmedia.fastfilm.feature.series

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.PagedProxy
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.EpisodeInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.network.apis.request.LatestEpisodeBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import kotlinx.coroutines.flow.Flow

/**
 * 剧集详情页
 */
class SeriesViewModel : AppRepositoryViewModel<SeriesRepository>() {
    val workId by lazy { mBundle.getString(Constants.Extra.ID) }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val workInfo by lazy { MutableLiveData<WorkInfo>() }

    //创建续集信息
    val episodeInfo = MutableLiveData<EpisodeInfo?>()

    override fun onCreate() {
        super.onCreate()
        getWorkDtl()
    }

    /**
     *作品详情
     */
    fun getWorkDtl() {
        loadState.set(LoadStateViewHolder.LOADING)
        if (workInfo.value == null) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                mRepo.getWorkInfo(workId).data?.let {
                    workInfo.value = it
                }
                updateLoadState()
            },
            onError = {
                updateLoadState()
            }
        )
    }

    private fun updateLoadState() {
        loadState.set(LoadStateViewHolder.CONTENT)
        if (workInfo.value == null) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    fun episodeList(): Flow<PagingData<EpisodeInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = PagedProxy.DEFAULT_PAGE_SIZE * 3,
                pageSize = PagedProxy.DEFAULT_PAGE_SIZE
            ),
            pagingSourceFactory = { EpisodePagingSource(workId, workInfo.value?.totalEpisodeCnt ?: 0) }
        ).flow.cachedIn(viewModelScope)
    }

    /**
     *获取剧集最新一集剧本详情
     */
    fun getEpisodeInfo() {
        launchWithLoading {
            response(
                mRepo.api.latestEpisode(
                    LatestEpisodeBody(
                        workType = LatestEpisodeBody.WORK_TYPE_INSPIRATION,
                        workId = workId
                    )
                )
            ) {
                episodeInfo.value = data
            }
        }
    }

    fun publishFailed(workInfo: WorkInfo?) = workInfo?.contractStatus == 3

    @ColorRes
    fun publishStatusColor(workInfo: WorkInfo?) = when (workInfo?.contractStatus) {
        /**[枚举]签约状态(0-未申请 1-审核中 2-已通过 3-已驳回)*/
        0 -> R.color.text_color_3_dark
        1 -> R.color.func_warning_dark
        2 -> when (workInfo.publishStatus) {
            /**[枚举]发行状态(0-未发行 1-待签约 2-发行上架中 3-已发行)*/
            0 -> R.color.text_color_3_dark
            1 -> R.color.func_warning_dark
            2 -> R.color.func_special_dark
            3 -> R.color.func_success_dark
            else -> R.color.func_failure_dark
        }

        3 -> R.color.func_failure_dark
        else -> R.color.func_failure_dark
    }

    fun publishStatusText(workInfo: WorkInfo?) = when (workInfo?.contractStatus) {
        /**[枚举]签约状态(0-未申请 1-审核中 2-已通过 3-已驳回)*/
        0 -> "未发行"
        1 -> "签约审核中"
        2 -> when (workInfo.publishStatus) {
            /**[枚举]发行状态(0-未发行 1-待签约 2-发行上架中 3-已发行)*/
            0 -> "未发行"
            1 -> "待签约"
            2 -> "发行上架中"
            3 -> "已发行"
            else -> "未知状态"
        }

        3 -> "签约失败"
        else -> "未知状态"
    }

    /**
     * @param contractLevel 签约评级（S/A/B/C等）
     */
    @DrawableRes
    fun workLevelIcon(contractLevel: String?) = when (contractLevel) {
        /**[枚举]签约等级(0-普通 1-专业 2-金牌)*/
        "S" -> R.drawable.icon_work_level_s
        "A" -> R.drawable.icon_work_level_a
        "B" -> R.drawable.icon_work_level_b
        "C" -> R.drawable.icon_work_level_c
        else -> 0
    }
}