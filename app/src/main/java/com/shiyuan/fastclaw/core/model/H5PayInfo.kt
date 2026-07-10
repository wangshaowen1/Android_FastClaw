package com.shiyuan.fastclaw.core.model

import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonParser

/**
 * H5 调用APP支付协议，由app调用接口获取支付参数并支付
 * APP 通过payWay 区分支付方式
 */
data class H5PayInfo(
    /**
     *  json字符串
     */
    val payInfo: JsonElement? = null,

    /**
     * wx||zfb
     */
    val payWay: String? = null,
    /**
     * 金额 单位厘
     */
    val amount: Long? = null
) {
    fun payInfoBody(): JsonElement? {
        val value = payInfo ?: return null
        if (value.isJsonPrimitive) {
            Log.d("payInfoBody",value.isJsonPrimitive.toString())
            val primitive = value.asJsonPrimitive
            if (primitive.isString) {
                return runCatching { JsonParser.parseString(primitive.asString) }.getOrNull() ?: value
            }
        }
        return value
    }
}
