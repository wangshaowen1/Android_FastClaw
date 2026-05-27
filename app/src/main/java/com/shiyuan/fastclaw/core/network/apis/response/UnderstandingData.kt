import com.google.gson.annotations.SerializedName

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
){

}

