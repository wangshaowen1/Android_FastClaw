package com.shiyuan.fastclaw.core.db.convert

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shiyuan.fastclaw.core.db.entity.AIMessageContent

class AIMessageContentTypeConverter {

    @TypeConverter
    fun stringToAiResponse(s: String?): AIMessageContent? =
            Gson().fromJson(s, object : TypeToken<AIMessageContent>() {}.type)

    @TypeConverter
    fun listToString(response: AIMessageContent?): String? = Gson().toJson(response)

}






