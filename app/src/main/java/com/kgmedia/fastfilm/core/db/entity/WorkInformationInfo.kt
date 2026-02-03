package com.kgmedia.fastfilm.core.db.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kgmedia.fastfilm.core.network.apis.response.WorkInformationData
import kotlinx.parcelize.Parcelize

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:作品信息
 * @Data: 2026/2/3/周二  9:40
 * @Version: 1.0
 * *****************************************
 */
@Parcelize
@Entity(tableName = "WorkInformationInfo")
data class WorkInformationInfo(
    @PrimaryKey(autoGenerate = true)
    var workInformationID: Long = 0,
    //作品id
    val workId: String,
    //作品本身的信息
    @Embedded
    val work: WorkInformationData,
) : Parcelable