package com.kgmedia.fastfilm.feature.series.share

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.request.UserShareUrlBody
import com.kgmedia.fastfilm.core.response
import com.nhsd.basic.ext.showToast

/**
 * 剧集-分享
 */
class SeriesShareViewModel : AppRepositoryViewModel<AppRepository>() {
    private val seriesId by lazy { mBundle.getString(Constants.Extra.ID) }
    private val authorUserId by lazy { mBundle.getString(Constants.Extra.USER_TOKEN) }
    val isMine by lazy { User.get().isSameId(authorUserId) }
    val loading by lazy { ObservableBoolean(false) }

    //分享链接
    private var shareUrl: String? = null

    fun getShareUrl(callback: ((shareUrl: String) -> Unit)?) {
        val url = shareUrl
        if (!url.isNullOrEmpty()) {
            callback?.invoke(url)
            return
        }
        loading.set(true)
        launch(block = {
            response(
                bean = mRepo.api.getUserShareUrl(UserShareUrlBody(2, seriesId, authorUserId)),
                result = {
                    loading.set(false)
                    val userShareUrl = data?.userShareUrl
                    if (!userShareUrl.isNullOrEmpty()) {
                        shareUrl = userShareUrl
                        callback?.invoke(userShareUrl)
                    }
                },
                error = {
                    loading.set(false)
                    this?.msg?.showToast()
                }
            )
        }, onError = {
            loading.set(false)
        })
    }

}