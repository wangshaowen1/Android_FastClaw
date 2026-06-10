package com.shiyuan.fastclaw.core.network.apis.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RegionLocation(
    val regionCode: String? = null,
    val regionName: String? = null,
    val regionLevel: Int? = null,
    val locationType: String? = null,
    val fullNamePath: String? = null,
    val firstLetter: String? = null,
) : Parcelable

data class RegionDictData(
    val maxSelectCount: Int? = null,
    val selectedLocations: List<RegionLocation>? = null,
    val historyLocations: List<RegionLocation>? = null,
    val hotLocations: List<RegionLocation>? = null,
    val groupedLocations: List<RegionGroupedLocation>? = null,
    val matchedLocations: List<RegionLocation>? = null,
    val alphabetIndex: List<String>? = null,
)

data class RegionGroupedLocation(
    val letter: String? = null,
    val locations: List<RegionLocation>? = null,
)
