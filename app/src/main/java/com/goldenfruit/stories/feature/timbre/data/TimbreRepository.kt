package com.kgmedia.fastfilm.feature.timbre.data

import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.request.TimbreAddOrOptimizeBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TimbreRepository : AppRepository() {

    suspend fun timbreAddOrOptimize(
        sourceSoundUrl: String?,
        userTimbreId: String?
    ) = withContext(Dispatchers.IO) {
        api.timbreAddOrOptimize(TimbreAddOrOptimizeBody(sourceSoundUrl, userTimbreId))
    }

//    suspend fun timbreCloneExampleMusicList() = withContext(Dispatchers.IO) {
//        api.timbreCloneExampleMusicList(PagedBody(1, Int.MAX_VALUE))
//    }
//
//    suspend fun deleteUserTimbre(timbreId: String) = withContext(Dispatchers.IO) {
//        api.deleteUserTimbre(TimbreIdBody(timbreId))
//    }
}