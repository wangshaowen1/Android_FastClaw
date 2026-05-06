package com.shiyuan.fastclaw.core.db.convert

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.shiyuan.fastclaw.core.network.apis.entity.EpisodeInfo
import com.shiyuan.fastclaw.core.network.apis.entity.PlatformInfo

/**
 * *****************************************
 * 项目名称: KgQuan
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2026/1/7/周三  11:23
 * @Version: 1.0
 * *****************************************
 */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromEpisodeInfoList(value: List<EpisodeInfo>?): String? {
        return value?.let {  Gson().toJson(it) }
    }

    @TypeConverter
    fun toEpisodeInfoList(value: String?): List<EpisodeInfo>? {
        return value?.let {
            val listType = object : TypeToken<List<EpisodeInfo>>() {}.type
            Gson().fromJson(value, listType)
        }
    }

    @TypeConverter
    fun fromPlatformList(value: List<PlatformInfo>?): String? {
        return value?.let {  Gson().toJson(it) }
    }

    @TypeConverter
    fun toPlatformList(value: String?): List<PlatformInfo>? {
        return value?.let {
            val listType = object : TypeToken<List<PlatformInfo>>() {}.type
            Gson().fromJson(value, listType)
        }
    }
}