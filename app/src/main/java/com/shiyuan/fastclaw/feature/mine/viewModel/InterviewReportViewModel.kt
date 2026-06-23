package com.shiyuan.fastclaw.feature.mine.viewModel

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
import com.shiyuan.fastclaw.feature.mine.InterviewReportPagingSource
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

    companion object {
        fun buildLayoutManager(context: Context?) = LinearLayoutManager(context).apply {
            isAutoMeasureEnabled = false
        }

        fun buildItemDecoration(context: Context?) = SpaceDecoration(12.dp)
    }
}
