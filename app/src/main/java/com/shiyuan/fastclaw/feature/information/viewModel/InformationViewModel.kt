package com.shiyuan.fastclaw.feature.information.viewModel

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.request.DictIndustryQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.DictPositionQueryBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumTreeUtil
import com.shiyuan.fastclaw.core.network.apis.response.FieldItems
import com.shiyuan.fastclaw.core.network.apis.response.IndustryDict
import com.shiyuan.fastclaw.core.response

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的
 * @Data: 2025/4/18  14:10
 * @Version: 1.0
 * *****************************************
 */
class InformationViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = "",
        background = Color.TRANSPARENT,
    )
    var allData = MutableLiveData<List<CommonEnumItem>>()
    var searchData = MutableLiveData<List<CommonEnumItem>>()
    var selectData = MutableLiveData<List<CommonEnumItem>>()
    var lastSelectedPosition = MutableLiveData<Int>(1)
    fun queryDictIndustryQuery(sessionId: String? = null,conversationId: String? = null) {
        launchWithLoading(true, block = {
            val requestBody = DictIndustryQueryBody(sessionId = sessionId, conversationId = conversationId)
            response(ApiFactory.apiService.queryDictIndustryQuery(requestBody), result = {
                allData.postValue(this.data?.items)
                if (selectData.value.isNullOrEmpty()) {
                    selectData.postValue(this.data?.selectedItems)
                }
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

    fun queryDictIndustryQuery(keyword: String? = null,sessionId: String? = null,conversationId: String? = null) {
        launchWithLoading(false, block = {
            val requestBody = DictIndustryQueryBody(
                sessionId = sessionId,
                conversationId = conversationId,
                keyword = keyword
            )
            response(ApiFactory.apiService.queryDictIndustryQuery(requestBody), result = {
                searchData.postValue(this.data?.items?.flatMap { it.children?:emptyList() } ?: emptyList())
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }



    var allPositioData = MutableLiveData<List<CommonEnumItem>>()
    var searchPositionData = MutableLiveData<List<CommonEnumItem>>()
    var selectPositionData = MutableLiveData<List<CommonEnumItem>>()
    var lastSelectedPosition2 = MutableLiveData<Int>(1)

    fun queryDictPositionQuery(sessionId: String? = null,conversationId: String? = null) {
        launchWithLoading(true, block = {
            val requestBody = DictPositionQueryBody(sessionId = sessionId, conversationId = conversationId)
            response(ApiFactory.apiService.queryDictPositionQuery(requestBody), result = {
                allPositioData.postValue(this.data?.firstCategories)
                if (selectData.value.isNullOrEmpty()) {
                    selectData.postValue(this.data?.selectedItems)
                }
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

    fun queryDictPositionQuery(keyword: String? = null,sessionId: String? = null,conversationId: String? = null) {
        launchWithLoading(false, block = {
            val requestBody = DictPositionQueryBody(keyword = keyword,sessionId = sessionId, conversationId = conversationId)
            response(ApiFactory.apiService.queryDictPositionQuery(requestBody), result = {
                searchPositionData.postValue(this.data?.matchedPositions)
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }



}