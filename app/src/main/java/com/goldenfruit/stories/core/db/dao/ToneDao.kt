package com.kgmedia.fastfilm.core.db.dao

import androidx.room.*
import com.kgmedia.fastfilm.core.db.entity.Tone

@Dao
interface ToneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(bean: List<Tone>)

    @Query("SELECT * FROM Tone ")
    fun queryAll(): List<Tone>?

    @Query("Delete from Tone")
    fun clear()
}