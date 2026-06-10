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
    var selectData = MutableLiveData<List<CommonEnumItem>>()
    var levelData = MutableLiveData<List<CommonEnumItem>>()
    var levelDataSong = MutableLiveData<List<CommonEnumItem>>()
    var lastSelectedPosition = MutableLiveData<Int>(1)
    fun queryDictIndustryQuery(keyword: String? = null) {
        launchWithLoading(true, block = {
            val requestBody = DictIndustryQueryBody(sessionId = "56317951643738",conversationId ="56317951643739",keyword = keyword)
            response(ApiFactory.apiService.queryDictIndustryQuery(requestBody), result = {
                allData.postValue(this.data?.items)
                if (selectData.value.isNullOrEmpty()) {
                    selectData.postValue(this.data?.selectedItems)
                }
                this.data?.items?.let {
                    val data = CommonEnumTreeUtil.filterByLevel(it , 1)
                    if (data.size > 1) { data[1].isSelected = true }
                    levelData.postValue(data)
                }
                this.data?.items?.let {
                    ///默认选中第2个
                    val data = CommonEnumTreeUtil.getChildrenByParentCode(it , this.data?.items?.get(lastSelectedPosition.value ?: 1)?.code.toString())

                    levelDataSong.postValue(data)
                }

            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

}