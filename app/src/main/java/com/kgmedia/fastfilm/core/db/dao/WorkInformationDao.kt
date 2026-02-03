package com.kgmedia.fastfilm.core.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kgmedia.fastfilm.core.db.entity.WorkInformationInfo

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:作品
 * @Data: 2026/2/3/周二  9:43
 * @Version: 1.0
 * *****************************************
 */
@Dao
interface WorkInformationDao {
    // 用于Paging3从数据库分页查询
    @Query("SELECT * FROM WorkInformationInfo  ORDER BY workInformationID ASC")
    fun getPosts(): PagingSource<Int, WorkInformationInfo>

    // 清空表（用于刷新）
    @Query("DELETE FROM WorkInformationInfo")
    suspend fun clearAll()

    // 插入帖子列表 如果插入失败（例如由于约束冲突且未使用 REPLACE策略），返回值可能为 -1。通过判断返回值是否大于 0，可以确定插入操作是否成功
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(complaintWorkOrderID: WorkInformationInfo)

    // 插入帖子列表
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<WorkInformationInfo>)

    @Query("SELECT * FROM WorkInformationInfo ORDER BY workId ASC")
    suspend fun getAll(): List<WorkInformationInfo>


    /**
     * ✅ 新增：根据 workId 列表批量删除
     */
    @Query("DELETE FROM WorkInformationInfo WHERE workId IN (:workIds)")
    suspend fun deleteByWorkIds(workIds: List<String>): Int
}