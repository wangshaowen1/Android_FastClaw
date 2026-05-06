package com.shiyuan.fastclaw.core.db.convert

import androidx.room.TypeConverter
import com.shiyuan.fastclaw.core.network.apis.entity.ShortDramaRoleInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RoleListTypeConverter {

    @TypeConverter
    fun stringToList(s: String?): List<ShortDramaRoleInfo>? =
        Gson().fromJson(s, object : TypeToken<List<String>>() {}.type)

    @TypeConverter
    fun listToString(list: List<ShortDramaRoleInfo>?): String? = Gson().toJson(list)

}






