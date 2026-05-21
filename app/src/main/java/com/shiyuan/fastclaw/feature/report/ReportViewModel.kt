package com.shiyuan.fastclaw.feature.report

import android.content.Context
import android.graphics.Color
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
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
    private var isLoading = false
    private val pageSize = 10

    override fun onCreate() {
        super.onCreate()
    }

     var schemeData = MutableLiveData<SchemeData>()

    fun getScheme(){
        setLoading(true)
        launch(block = {
            response(
                ApiFactory.apiService.getScheme(),
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

    fun loadData(isRefresh: Boolean = true) {
        if (isLoading) return
        isLoading = true

        val targetPage = if (isRefresh) 1 else currentPage + 1
        val currentState = loadState.get()

        if (isRefresh) {
            loadState.set(LoadState.Loading)
        } else if (currentState is LoadState.NotLoading && currentState.endOfPaginationReached) {
            return
        }

        request(
            apiBlock = { ApiFactory.apiService.getMyReportList(PagedBody(targetPage, pageSize)) },
            onSuccess = {
                isLoading = false
                data?.let { data ->
                    val newData = data.toReportInfoList()
                    if (isRefresh) {
                        currentPage = 1
                        listData = newData.toMutableList()
                        refreshList()
                        val isEmpty = newData.isEmpty()
                        loadState.set(
                            if (isEmpty) LoadState.Empty
                            else LoadState.NotLoading(isEmpty)
                        )
                    } else {
                        if (newData.isEmpty()) {
                            loadState.set(LoadState.NotLoading(true))
                        } else {
                            currentPage = targetPage
                            listData.addAll(newData)
                            refreshList()
                            loadState.set(LoadState.NotLoading(false))
                        }
                    }
                } ?: run {
                    isLoading = false
                    if (isRefresh) {
                        listData.clear()
                        refreshList()
                        loadState.set(LoadState.Empty)
                    } else {
                        loadState.set(LoadState.NotLoading(true))
                    }
                }
            },
            onError = {
                isLoading = false
                if (isRefresh) {
                    loadState.set(LoadState.Error(it ?: Exception("unknown")))
                }
            }
        )
    }

    fun refresh() = loadData(isRefresh = true)

    fun loadMore() = loadData(isRefresh = false)

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


    companion object {
        fun buildLayoutManager(context: Context?) = LinearLayoutManager(context).apply {
            isAutoMeasureEnabled = false
        }

        fun buildItemDecoration(context: Context?) = SpaceDecoration(12.dp)
    }
}