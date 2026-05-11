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
import com.shiyuan.fastclaw.feature.report.data.ReportStatus
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
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

    private var listData = mutableListOf<ReportInfo>()

    override fun onCreate() {
        super.onCreate()
        loadData()
    }

    fun loadData() {
        loadState.set(LoadState.Loading)
        request(
            apiBlock = { ApiFactory.apiService.getMyReportList(UserTokenBody()) },
            onSuccess = {
                data?.let { data ->
                    listData = data.toReportInfoList().toMutableList()
                    refreshList()
                    loadState.set(
                        if (data.toReportInfoList().isEmpty()) LoadState.Empty
                        else LoadState.NotLoading(true)
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
        loadData()
    }

    private fun refreshList() {
        val items = listData.map { info ->
            ItemReportVM(info).apply {
                onItemClick.value = {
                    onReportItemClick(info)
                }
            }
        }
        listAdapter.submitList(items)
    }

    fun onReportItemClick(info: ReportInfo) {
        // 报告详情跳转
        when (info.status) {
            ReportStatus.GENERATING -> {
                // 生成中的报告，暂不可查看
            }
            ReportStatus.COMPLETED -> {
                // TODO: 跳转到报告详情页面
            }
        }
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
