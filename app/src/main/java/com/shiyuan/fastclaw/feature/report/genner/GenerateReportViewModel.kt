package com.shiyuan.fastclaw.feature.report.genner

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
import com.shiyuan.fastclaw.feature.report.data.ReportStatus
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.ResumeMaterialQualityBody
import com.shiyuan.fastclaw.core.network.apis.request.UserTokenBody
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import okhttp3.MultipartBody

/**
 * @Introduce: 历史报告列表 ViewModel
 */
class GenerateReportViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.generate_report.getResString(),
        background = Color.TRANSPARENT,
    )

    val resumeData = MutableLiveData<ResumeData>()

    fun resumeMaterialQuality(resumeFileUrl: String) {
        launchWithLoading(true, block = {
            response(
                mRepo.api.resumeMaterialQuality(
                ResumeMaterialQualityBody(
                    resumeFileUrl = resumeFileUrl
                )
            ), result = {
                 setLoading(false)
                resumeData.postValue(this.data)
            }, error = { this?.msg?.showToast() } )
        }, onError = { it.message.toString().showToast() })

    }


    fun resumeMaterialQualityr() {
        launchWithLoading(true, block = {
            response(
                mRepo.api.resumeMaterialQualityResult(
                ResumeMaterialQualityBody(
                    checkId = resumeData.value?.checkId
                )
            ), result = {
                 if(this.data?.checkStatus != "RUNNING"){
                     setLoading(false)
                 }
                resumeData.postValue(this.data)
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }


    override fun onCreate() {
        super.onCreate()


    }


}
