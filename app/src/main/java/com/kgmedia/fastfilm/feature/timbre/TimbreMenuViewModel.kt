package com.kgmedia.fastfilm.feature.timbre

import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.TimbreInfo
import com.kgmedia.fastfilm.core.network.apis.request.UserTimbreIdBody
import com.kgmedia.fastfilm.core.response

class TimbreMenuViewModel : AppRepositoryViewModel<AppRepository>() {

    val timbreInfo by lazy { mBundle.getParcelable<TimbreInfo>(Constants.Extra.TIMBRE) }

    /**
     * 删除音色
     */
    fun deleteTimbre(userTimbreId: String?, callback: () -> Unit) {
        launchWithLoading {
            response(mRepo.api.delTimbre(UserTimbreIdBody(userTimbreId = userTimbreId))) {
                callback()
            }
        }
    }
}