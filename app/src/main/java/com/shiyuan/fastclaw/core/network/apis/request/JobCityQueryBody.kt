package com.shiyuan.fastclaw.core.network.apis.request

/**
 * 城市选择器查询请求体。
 *
 * 用于按业务场景、关键字、已选城市等条件查询城市/区县候选列表。
 */
data class JobCityQueryBody(
    /** 城市字段场景编码：location-现居住城市，expectedCity-意向城市；服务端按该值反显候选人当前已选城市。 */
    var sceneCode: String = EXPECTED_CITY,
    /** 城市、区县中文名或拼音关键字。 */
    var keyword: String? = null,
    /** 当前已选择的城市/区县编码列表。 */
    var selectedRegionCodes: List<String>? = null,
    /** 是否返回热门地点，默认 true。 */
    var includeHotLocations: Boolean = true,
    /** 是否返回当前用户历史选择地点，默认 true。 */
    var includeHistoryLocations: Boolean = true,
    /** 最大可选地点数，默认 3。 */
    var maxSelectCount: Int = 3,
) {
    companion object {

        /** 现居住城市场景编码。 */
        const val LOCATION = "location"
        /** 意向城市场景编码。 */
        const val EXPECTED_CITY = "expectedCity"
    }
}
