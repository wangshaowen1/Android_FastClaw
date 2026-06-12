package com.shiyuan.fastclaw.core.network.apis.response

import com.shiyuan.fastclaw.core.network.apis.entity.SimpleMsgAgent
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import kotlinx.serialization.json.Json

/**
 * 城市选择器查询响应数据。
 */
data class JobCityData(
    /** 最大可选地点数。 */
    val maxSelectCount: Int? = null,
    /** 服务端按场景反显的当前已选地点列表。 */
    val selectedLocations: List<CityLocation>? = null,
    /** 当前用户历史选择地点列表。 */
    val historyLocations: List<CityLocation>? = null,
    /** 热门地点列表。 */
    val hotLocations: List<CityLocation>? = null,
    /** 按拼音首字母分组的地点列表。 */
    val groupedLocations: List<GroupLocation>? = null,
    /** 关键字搜索命中的地点列表。 */
    val matchedLocations: List<CityLocation>? = null,
    /** 右侧字母索引列表。 */
    val alphabetIndex: List<String>? = null,
) {
    companion object {
        fun toJson(type: String, data: LinkedHashMap<String, CityLocation>): String? {
            return if (type == JobCityQueryBody.EXPECTED_CITY) {
                Json.encodeToString(
                    data.values.map { city ->
                        SimpleMsgAgent(code = city.regionCode, name = city.regionName)
                    }
                )
            } else {
                val first = data.values.first()
                Json.encodeToString(
                    SimpleMsgAgent(code = first.regionCode, name = first.regionName)
                )
            }
        }
    }
}

/**
 * 城市/区县字母分组。
 */
data class GroupLocation(
    /** 分组字母。 */
    val letter: String? = null,
    /** 当前字母分组下的地点列表。 */
    val locations: List<CityLocation>? = null,
)

/**
 * 城市/区县地点信息。
 */
data class CityLocation(
    /** 行政区划编码。 */
    val regionCode: String? = null,
    /** 行政区划名称。 */
    val regionName: String? = null,
    /** 层级，工作地点选择器通常返回地级或县级。 */
    val regionLevel: Int? = null,
    /** 地点类型。 */
    val locationType: String? = null,
    /** 完整名称路径。 */
    val fullNamePath: String? = null,
    /** 拼音首字母。 */
    val firstLetter: String? = null,
)

