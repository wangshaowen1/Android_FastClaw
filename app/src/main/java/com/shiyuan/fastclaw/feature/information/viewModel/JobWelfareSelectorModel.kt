package com.shiyuan.fastclaw.feature.information.viewModel

import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.parseMessage
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.request.DictWelfareQueryBody
import com.shiyuan.fastclaw.core.network.apis.response.WelfareDict
import com.shiyuan.fastclaw.core.response

class JobWelfareSelectorModel : AppRepositoryViewModel<AppRepository>() {

    val welfareDict = MutableLiveData<WelfareDict>()

    fun queryWelfareDict(
        sessionId: String? = null,
        conversationId: String? = null,
        keyword: String? = null
    ) {
        launch(block = {
            val requestBody = DictWelfareQueryBody(
                sessionId = sessionId,
                conversationId = conversationId,
                keyword = keyword,
                limit = 100,
            )
            response(
                ApiFactory.apiService.queryDictWelfareQuery(requestBody),
                result = {
                    data?.let { welfareDict.postValue(it) }
                },
                error = {
                    this?.msg?.showToast()
                },
            )
        }, onError = {
            parseMessage(it)?.showToast()
        }, onComplete = {
            setLoading(false)
        })
    }
}
