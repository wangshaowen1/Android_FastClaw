package com.kgmedia.fastfilm.core.db.dao

import androidx.room.*
import com.kgmedia.fastfilm.core.db.entity.SearchHistory

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bean: SearchHistory)

    @Query("SELECT * FROM SearchHistory order by createTime desc limit 20")
    fun queryAll(): List<SearchHistory>?

    @Query("Delete from SearchHistory")
    fun clear()
}