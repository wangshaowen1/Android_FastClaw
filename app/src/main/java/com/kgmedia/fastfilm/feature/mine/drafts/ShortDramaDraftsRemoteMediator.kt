package com.kgmedia.fastfilm.feature.mine.drafts

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.db.AppDatabase
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.request.PagedBody
import com.nhsd.basic.utils.Logger

/**
 * *****************************************
 * 项目名称: KgQuan
 * @Author: SZYMN
 * @Introduce:绘本作品
 * @Data: 2025/7/14/周一  10:24
 * @Version: 1.0
 * *****************************************
 */
@OptIn(ExperimentalPagingApi::class)
class ShortDramaDraftsRemoteMediator(
    private val database: AppDatabase,
    private val onTotalCountUpdate: (Int) -> Unit = {}
) : RemoteMediator<Int, ShortDramaInfo>() {

    private val shortDramsDao = database.shortDramsDao


    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ShortDramaInfo>
    ): MediatorResult {
        return try {
            if (User.get().isLogged.not()) {
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            // 根据加载类型决定页码
            val page = when (loadType) {
                LoadType.REFRESH -> {
                    // 刷新操作，通常从头开始加载或根据策略决定
                    1
                }

                LoadType.PREPEND -> {
                    // 向前加载（加载更旧的数据），通常使用第一页的 prevKey
                    // 如果假设不需要向前加载，返回 null 表示结束
                    return MediatorResult.Success(endOfPaginationReached = true)
                }

                LoadType.APPEND -> {
                    // 获取当前已加载的最后一项，从中得到下一页的页码
                    val lastItem = state.lastItemOrNull()
                    if (lastItem == null) {
                        1 // 如果没有加载过任何项，也从第1页开始
                    } else {
                        // 假设你的 lastItem 里有一个 page 字段记录它属于哪一页
                        // 或者，更常见的，你根据 state 的配置和已加载项的数量来计算
                        // 这里是一种简单的计算方式：最后一页的页码 + 1
                        state.pages.size + 1
                    }
                }
            }

            val body = PagedBody(page, state.config.pageSize)
            val response = ApiFactory.apiService.userShortDramaDrafts(body)

            if (response.isSuccess.not()) {
                return MediatorResult.Error(ApiException(response.code, response.msg))
            }

            // 从网络获取数据
            val data = response.data?.items
            val totalCount= response.data?.totalSize
            Logger.i("total_size0=0="+totalCount)
            // 回调传递总数
            onTotalCountUpdate(totalCount?:0)
            // 在数据库事务中处理数据
            database.withTransaction {
                if (page <= 1) {
                    // 刷新时清空旧数据
                    shortDramsDao.clearAll()
                }
                // 插入新数据
                data?.toList()?.let { shortDramsDao.insertAll(it) }
            }
            // 加载下一页的键（为 null 表示没有下一页） true 表示没有更多数据了
            val endOfPaginationReached =data.isNullOrEmpty() || data.size < state.config.pageSize
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}