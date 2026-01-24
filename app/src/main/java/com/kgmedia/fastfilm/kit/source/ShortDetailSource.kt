package com.kgmedia.fastfilm.kit.source

import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo

/**
 * Interface defining the contract for a data source of short.
 * Implementations of this interface must provide a method to load short information.
 */
interface ShortDetailSource {

    /**
     * Loads short information.
     * @return An [com.kgmedia.fastfilm.core.network.apis.ApiResponse] containing a [com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo] or null if the data is not available.
     *         The [ApiResponse] will also indicate whether the request was successful or if an error occurred.
     */
    suspend fun loadDetail(shortDramaId: String): ApiResponse<ShortDramaInfo?>
}