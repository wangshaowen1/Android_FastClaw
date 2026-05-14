package com.shiyuan.fastclaw.feature.report

import android.content.Context
import android.graphics.Color
import androidx.databinding.ObservableField
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
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import okhttp3.MultipartBody

/**
 * @Introduce: 历史报告列表 ViewModel
 */
class ReportViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.report_my_name.getResString(),
        background = Color.TRANSPARENT,
    )

    val loadState = ObservableField<LoadState>(LoadState.NotLoading(false))

    val listAdapter = QuickAdapter<ItemReportVM>(R.layout.item_report, emptyList())

    // 外部注入的 item 点击回调（由 Activity 设置）
    var itemClickListener: ((ReportInfo) -> Unit)? = null

    private var listData = mutableListOf<ReportInfo>()
    private var currentPage = 1
    private var isLoadingMore = false
    private val pageSize = 10

    override fun onCreate() {
        super.onCreate()
    }

    fun loadData() {
        loadState.set(LoadState.Loading)
        request(
            apiBlock = { ApiFactory.apiService.getMyReportList(PagedBody(1, pageSize)) },
            onSuccess = {
                data?.let { data ->
                    currentPage = 1
                    listData = data.toReportInfoList().toMutableList()
                    refreshList()
                    val isEmpty = data.toReportInfoList().isEmpty()
                    loadState.set(
                        if (isEmpty) LoadState.Empty
                        else LoadState.NotLoading(isEmpty)
                    )
                } ?: run {
                    listData.clear()
                    refreshList()
                    loadState.set(LoadState.Empty)
                }
            },
            onError = {
                loadState.set(LoadState.Error(it ?: Exception("unknown")))
            }
        )
    }

    fun refresh() {
        currentPage = 1
        isLoadingMore = false
        loadData()
    }

    fun loadMore() {
        if (isLoadingMore) return
        val currentState = loadState.get()
        if (currentState is LoadState.NotLoading && currentState.endOfPaginationReached) return

        isLoadingMore = true
        val nextPage = currentPage + 1
        request(
            apiBlock = { ApiFactory.apiService.getMyReportList(PagedBody(nextPage, pageSize)) },
            onSuccess = {
                isLoadingMore = false
                data?.let { data ->
                    val newData = data.toReportInfoList()
                    if (newData.isEmpty()) {
                        loadState.set(LoadState.NotLoading(true))
                    } else {
                        currentPage = nextPage
                        listData.addAll(newData)
                        refreshList()
                        loadState.set(LoadState.NotLoading(false))
                    }
                } ?: run {
                    loadState.set(LoadState.NotLoading(true))
                }
            },
            onError = {
                isLoadingMore = false
            }
        )
    }

    private fun refreshList() {
        val items = listData.map { info ->
            ItemReportVM(info).apply {
                // 使用外部回调设置点击事件（如果回调不为空）
                onItemClick.value = if (itemClickListener != null) {
                    { itemClickListener?.invoke(info) }
                } else {
                    null
                }
            }
        }
        listAdapter.submitList(items)
    }

    /**
     * 上传简历
     * @param file 文件 MultipartBody
     * @param fileName 文件名
     * @param callback 上传结果回调
     */
    fun uploadResume(file: MultipartBody.Part, fileName: String, callback: (Boolean) -> Unit) {
        request(
            apiBlock = { ApiFactory.apiService.uploadResume(file, fileName) },
            onSuccess = {
                callback(true)
            },
            onError = {
                callback(false)
            }
        )
    }

    companion object {
        fun buildLayoutManager(context: Context?) = LinearLayoutManager(context).apply {
            isAutoMeasureEnabled = false
        }

        fun buildItemDecoration(context: Context?) = SpaceDecoration(12.dp)
    }
}