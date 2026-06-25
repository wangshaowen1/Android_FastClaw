package com.shiyuan.fastclaw.common.utils

import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * @desc：JsonUtil
 * @author：CaoWei
 * @date：2025/3/1 15:40
 */
object JsonUtil {

    fun <T> parseList(jsonString: String, classZ: Class<T>): List<T>? {
        try {
            return JSONArray.parseArray(jsonString, classZ)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun <T> parseObject(jsonString: String, classZ: Class<T>): T? {
        try {
            return JSONObject.parseObject(jsonString, classZ)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    inline fun <reified T> fromJson(json: String): T? {
        try {
            return Gson().fromJson(json,  object : TypeToken<T>() {}.type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun toJson(params: Any?): String {
        return Gson().toJson(params)
    }


}