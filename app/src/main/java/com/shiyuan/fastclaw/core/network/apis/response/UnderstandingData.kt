import com.google.gson.annotations.SerializedName

/**
 *  理解分数
 */
class UnderstandingScore(
    @SerializedName("understanding_score")
    var understandingScore: Int = 0
)


class UnderstandingDetail(
    @SerializedName("resume_info")
    var resumeInfo: List<FieldItems> = emptyList(),
    @SerializedName("job_preference")
    var jobPreference: List<FieldItems> = emptyList(),
    @SerializedName("job_info")
    var titleInfo: List<FieldItems> = emptyList()
)

class FieldItems(
    @SerializedName("field_code")
    var fieldCode: String? = null,
    @SerializedName("field_name")
    var fieldName: String? = null,
    @SerializedName("field_type")
    var fieldType: String? = null,
    @SerializedName("field_value")
    var fieldValue: String? = null,
    @SerializedName("field_items")
    var fieldItems: List<FieldItems> = emptyList()
){

}

