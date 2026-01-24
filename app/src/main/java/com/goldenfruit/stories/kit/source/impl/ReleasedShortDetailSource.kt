package com.kgmedia.fastfilm.kit.source.impl

import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.ApiResponse
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.request.DramasIdBody
import com.kgmedia.fastfilm.kit.source.ShortDetailSource


class ReleasedShortDetailSource() : ShortDetailSource {
    override suspend fun loadDetail(shortDramaId: String): ApiResponse<ShortDramaInfo?> {
        return ApiFactory.apiService.releasedShortDramasInfo(DramasIdBody(shortDramaId))
    }

}