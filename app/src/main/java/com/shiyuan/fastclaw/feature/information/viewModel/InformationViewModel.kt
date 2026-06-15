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
import com.shiyuan.fastclaw.core.network.apis.response.AttributeSave
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
/**
 * 求职信息填写相关页面共享的 ViewModel：
 * 行业 / 职位字典查询、字段保存等能力。
 */
class InformationViewModel : AppRepositoryViewModel<AppRepository>() {

    // 顶部导航 ViewModel（占位，未实际绑定 Toolbar）
    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = "",
        background = Color.TRANSPARENT,
    )
    // 行业字典：完整一级 + 二级树
    var allData = MutableLiveData<List<CommonEnumItem>>()
    // 行业搜索结果
    var searchData = MutableLiveData<List<CommonEnumItem>>()
    // 多选模式下用户已选行业
    var selectData = MutableLiveData<List<CommonEnumItem>>()
    // 上次选中的一级行业索引（用于恢复选中态）
    var lastSelectedPosition = MutableLiveData<Int>(1)

    /**
     * 请求行业字典（无关键词，带 Loading）。
     */
    fun queryDictIndustryQuery(sessionId: String? = null, conversationId: String? = null) {
        launchWithLoading(true, block = {
            val requestBody =
                DictIndustryQueryBody(sessionId = sessionId, conversationId = conversationId)
            response(ApiFactory.apiService.queryDictIndustryQuery(requestBody), result = {
                allData.postValue(this.data?.items)
                // 首次进入时同步服务端反显的已选项
                if (selectData.value.isNullOrEmpty()) {
                    selectData.postValue(this.data?.selectedItems)
                }
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

    /**
     * 请求行业字典（带关键词，用于搜索）。
     */
    fun queryDictIndustryQuery(
        keyword: String? = null,
        sessionId: String? = null,
        conversationId: String? = null
    ) {
        launchWithLoading(false, block = {
            val requestBody = DictIndustryQueryBody(
                sessionId = sessionId,
                conversationId = conversationId,
                keyword = keyword
            )
            response(ApiFactory.apiService.queryDictIndustryQuery(requestBody), result = {
                // 搜索结果取所有一级节点下的二级节点并扁平化
                searchData.postValue(this.data?.items?.flatMap { it.children ?: emptyList() }
                    ?: emptyList())
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

    // 职位字典：完整一级分类
    var allPositioData = MutableLiveData<List<CommonEnumItem>>()
    // 职位搜索结果
    var searchPositionData = MutableLiveData<List<CommonEnumItem>>()
    // 多选模式下用户已选职位
    var selectPositionData = MutableLiveData<List<CommonEnumItem>>()
    // 上次选中的一级职位索引
    var lastSelectedPosition2 = MutableLiveData<Int>(1)

    /**
     * 请求职位字典（无关键词，带 Loading）。
     */
    fun queryDictPositionQuery(sessionId: String? = null, conversationId: String? = null) {
        launchWithLoading(true, block = {
            val requestBody =
                DictPositionQueryBody(sessionId = sessionId, conversationId = conversationId)
            response(ApiFactory.apiService.queryDictPositionQuery(requestBody), result = {
                allPositioData.postValue(this.data?.firstCategories)
                // 注意：此处用的是 selectData 而非 selectPositionData，沿用原实现
                if (selectData.value.isNullOrEmpty()) {
                    selectData.postValue(this.data?.selectedItems)
                }
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

    /**
     * 请求职位字典（带关键词，用于搜索）。
     */
    fun queryDictPositionQuery(
        keyword: String? = null,
        sessionId: String? = null,
        conversationId: String? = null
    ) {
        launchWithLoading(false, block = {
            val requestBody = DictPositionQueryBody(
                keyword = keyword,
                sessionId = sessionId,
                conversationId = conversationId
            )
            response(ApiFactory.apiService.queryDictPositionQuery(requestBody), result = {
                searchPositionData.postValue(this.data?.matchedPositions)
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })
    }

    // 字段保存接口的返回结果
    var attributeSave = MutableLiveData<AttributeSave>()

    /**
     * 调用字段保存接口。data 直接以 Any 透传给底层接口，便于支持多种数据类型。
     */
    fun attributeSave(data: Any) {
        launchWithLoading(false, block = {
            response(ApiFactory.apiService.attributeSave(data), result = {
                attributeSave.postValue(this.data)
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })
    }


}
