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

/**
 * 福利偏好选择页的 ViewModel。
 * 调用福利字典接口，承载候选项与服务端反显的已选项。
 */
class JobWelfareSelectorModel : AppRepositoryViewModel<AppRepository>() {

    // 福利字典接口返回的完整数据
    val welfareDict = MutableLiveData<WelfareDict>()

    /**
     * 请求福利偏好字典数据。
     *
     * @param sessionId 会话 ID。
     * @param conversationId 对话 ID。
     * @param keyword 搜索关键词（暂未使用，保留参数）。
     */
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
                    // 把响应数据回传 View 层
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
