package com.shiyuan.fastclaw.feature.information.viewModel

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
class JobCitySelectorModel : AppRepositoryViewModel<AppRepository>() {

    val jobCityList = MutableLiveData<JobCityData>()

    val mToolbarVM by lazy {
        ToolbarViewModel(
            navigationAction = { finish() },
            title = "",
            background = Color.TRANSPARENT,
        )
    }

    fun getJobCityList(params: JobCityQueryBody) {
            launch(block = {
                response(
                    ApiFactory.apiService.queryCityList(params),
                    result = {
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