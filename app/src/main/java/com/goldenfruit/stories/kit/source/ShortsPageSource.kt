package com.kgmedia.fastfilm.kit.source

import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.PagedList
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
/**
 * Interface defining the contract for a paginated data source of shorts.
 * Implementations of this interface must provide a method to load the next page of short information.
 */
interface ShortsPageSource {

    /**
     * Loads the next page of short information.
     *
     * @param page The page number to load. Page numbers start at 1.
     * @param pageSize The number of items to load per page.
     * @return An [ApiResponse] containing a [PagedList] of [ShortDramaInfo] or null if the data is not available.
     *         The [ApiResponse] will also indicate whether the request was successful or if an error occurred.
     */
    suspend fun nextPage(page: Int, pageSize: Int): ApiResponse<PagedList<ShortDramaInfo>?>

    val sourceId: String get() = this.javaClass.simpleName

    fun makeSourceId(vararg id: String?): String {
        return "${this.javaClass.simpleName}_${id.joinToString("_")}"
    }
}
