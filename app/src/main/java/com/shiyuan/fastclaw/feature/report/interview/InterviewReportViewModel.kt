package com.shiyuan.fastclaw.feature.report.interview

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.request.InterviewReportDeleteBody
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

class InterviewReportViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.report_my_name.getResString(),
        background = Color.TRANSPARENT,
    )

    val reportListFlow = Pager(
        PagingConfig(pageSize = 10)
    ) {
        InterviewReportPagingSource(mRepo.api)
    }.flow
        .flowOn(Dispatchers.Default)
        .cachedIn(viewModelScope)

    fun hideReport(
        reportId: String,
        reportType: String?,
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        if (reportId.isBlank() || reportType.isNullOrBlank()) {
            onError()
            return
        }
        requestWithLoading(
            apiBlock = {
                mRepo.api.deleteInterviewReport(
                    InterviewReportDeleteBody(
                        reportType = reportType,
                        reportId = reportId,
                    )
                )
            },
            showToastError = true,
            onError = {
                onError.invoke()
            }
        ) {
            onSuccess()
        }
    }

    companion object {
        fun buildLayoutManager(context: Context?) = LinearLayoutManager(context).apply {
            isAutoMeasureEnabled = false
        }

        fun buildItemDecoration(context: Context?) = SpaceDecoration(12.dp)
    }
}
