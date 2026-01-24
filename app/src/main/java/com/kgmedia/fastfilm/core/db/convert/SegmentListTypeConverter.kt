package com.kgmedia.fastfilm.core.db.convert

import androidx.room.TypeConverter
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SegmentListTypeConverter {

    @TypeConverter
    fun stringToList(s: String?): List<ShortDramaSegmentInfo>? =
        Gson().fromJson(s, object : TypeToken<List<String>>() {}.type)

    @TypeConverter
    fun listToString(list: List<ShortDramaSegmentInfo>?): String? = Gson().toJson(list)

}






