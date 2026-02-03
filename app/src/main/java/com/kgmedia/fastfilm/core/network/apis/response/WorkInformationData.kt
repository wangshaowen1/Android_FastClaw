package com.kgmedia.fastfilm.core.network.apis.response

import android.os.Parcelable
import androidx.room.TypeConverters
import com.kgmedia.fastfilm.core.db.convert.Converters
import kotlinx.parcelize.Parcelize

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:作品信息
 * @Data: 2026/2/3/周二  9:46
 * @Version: 1.0
 * *****************************************
 */
@Parcelize
@TypeConverters(Converters::class)
data class WorkInformationData (
    val id: String ?,
): Parcelable{
    companion object {
        //草稿状态
        const val DRAFT_STATE = 1001

        //待合成状态
        const val WAIT_STATE = 1002

        //合成中状态
        const val COMPOSING_STATE = 1003

        //已完成状态
        const val COMPLETED_STATE = 1004
    }
}