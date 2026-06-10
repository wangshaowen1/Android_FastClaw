package com.shiyuan.fastclaw.core.network.apis.request

data class JobCityQueryBody(
    var sceneCode: String = EXPECTED_CITY,
    var keyword: String? = null,
    var selectedRegionCodes: List<String>? = null,
    var includeHotLocations: Boolean = true,
    var includeHistoryLocations: Boolean = true,
    var maxSelectCount: Int = 3,
) {
    companion object {

        const val LOCATION = "location"
        const val EXPECTED_CITY = "expectedCity"
    }
}
