package com.shiyuan.fastclaw.feature.report

import android.content.Context
import android.graphics.Color
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.common.LoadState
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.network.apis.response.toReportInfoList
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.parseMessage
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.entity.SchemeData
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.shrimp.UserPointPagingSource
import com.shiyuan.fastclaw.feature.report.data.ReportPagingSource
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import okhttp3.MultipartBody
import java.util.UUID

/**
 * @Introduce: 历史报告列表 ViewModel
 */
class ReportViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.report_my_name.getResString(),
        background = Color.TRANSPARENT,
    )

    val reportListFlow = Pager(
        PagingConfig(pageSize = 10)
    ) {
        ReportPagingSource()
    }.flow
        .flowOn(Dispatchers.Default)
        .cachedIn(viewModelScope)


     var schemeData = MutableLiveData<SchemeData>()

    fun getScheme(bizType: String? = null){
        setLoading(true)
        launch(block = {
            response(
                ApiFactory.apiService.getScheme(mapOf("bizType" to bizType)),
                result = {
                    schemeData.postValue(this.data)
                },
                error = {
                    this?.msg?.showToast()
                })
        }, onError = {
            parseMessage(it)?.showToast()
        }, onComplete = {
            setLoading(false)
        })
    }




    companion object {
        fun buildLayoutManager(context: Context?) = LinearLayoutManager(context).apply {
            isAutoMeasureEnabled = false
        }

        fun buildItemDecoration(context: Context?) = SpaceDecoration(12.dp)
    }
}