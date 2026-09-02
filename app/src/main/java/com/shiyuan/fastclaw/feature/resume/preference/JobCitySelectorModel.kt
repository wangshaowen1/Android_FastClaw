package com.shiyuan.fastclaw.feature.resume.preference

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.parseMessage
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import com.shiyuan.fastclaw.core.network.apis.response.JobCityData
import com.shiyuan.fastclaw.core.response

/**
 * @desc：工作城市选择
 * @author：CaoWei
 * @date：2026/6/10 13:50
 */
/**
 * 工作城市选择页的 ViewModel：
 * 负责调用城市列表接口，承载分组、热门、历史、搜索结果和反显已选项数据。
 */
class JobCitySelectorModel : AppRepositoryViewModel<AppRepository>() {

    // 城市列表数据
    val jobCityList = MutableLiveData<JobCityData>()

    // 顶部导航 ViewModel（占位）
    val mToolbarVM by lazy {
        ToolbarViewModel(
            navigationAction = { finish() },
            title = "",
            background = Color.TRANSPARENT,
        )
    }

    /**
     * 请求城市列表数据。
     * params 中包含场景编码（现居住城市 / 意向城市）和搜索关键词。
     */
    fun getJobCityList(params: JobCityQueryBody) {
            launch(block = {
                response(
                    ApiFactory.apiService.queryCityList(params),
                    result = {
                        // 接口返回后通知 View 层刷新
                        jobCityList.postValue(this.data)
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
}
