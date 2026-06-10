package com.shiyuan.fastclaw.core.network.apis.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class JobCityData(
    val maxSelectCount: Int? = null,
    val selectedLocations: List<CityLocation>? = null,
    val historyLocations: List<CityLocation>? = null,
    val hotLocations: List<CityLocation>? = null,
    val groupedLocations: List<GroupLocation>? = null,
    val matchedLocations: List<CityLocation>? = null,
    val alphabetIndex: List<String>? = null,
) {
    companion object {
        fun toJson(data: LinkedHashMap<String, CityLocation>): String? {
            val jsonString = Json.encodeToString(
                data.values.map { city ->
                    SimpleLocation(code = city.regionCode, name = city.regionName)
                }
            )
            return jsonString
        }
    }
}

data class GroupLocation(
    val letter: String? = null,
    val locations: List<CityLocation>? = null,
)

data class CityLocation(
    val regionCode: String? = null,
    val regionName: String? = null,
    val regionLevel: Int? = null,
    val locationType: String? = null,
    val fullNamePath: String? = null,
    val firstLetter: String? = null,
)

@Serializable
data class SimpleLocation(
    val code: String?,
    val name: String?
)

