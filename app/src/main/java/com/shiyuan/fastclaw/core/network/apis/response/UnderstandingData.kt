package com.shiyuan.fastclaw.core.network.apis.response


/**
 *  理解分数
 */
class UnderstandingScore(
    var understandingScore: Int = 0
)


class UnderstandingDetail(
    var resumeInfo: List<FieldItems> = emptyList(),
    var jobPreference: List<FieldItems> = emptyList(),
    var titleInfo: List<FieldItems> = emptyList()
)

class FieldItems(
    var fieldCode: String? = null,
    var fieldName: String? = null,
    var fieldType: String? = null,
    var fieldValue: Any? = null,
    var fieldItems: List<FieldItems> = emptyList(),
    val extra: Map<String, Any>? = null   // 用于存放额外标记
){}

fun formatValueToString(value: Any?): String {
    return when (value) {
        null -> ""
        is Number -> value.toString().replace(Regex("\\.0+$"), "") ?: ""
        is Boolean -> if (value) "是" else "否"
        else -> value.toString()
    }
}

