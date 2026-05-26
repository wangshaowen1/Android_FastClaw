package com.shiyuan.fastclaw.feature.report.report

import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.common.ext.cleanupTempFile
import com.shiyuan.fastclaw.common.ext.isNetworkPath
import com.shiyuan.fastclaw.common.ext.toOssFilePath
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.loginToast
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeMaterialQualityBody
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.report.data.ReportRepository
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.FAILED
import com.shiyuan.fastclaw.kit.SignUtils
import okio.Path

/**
 * @Introduce:
 */
class HomeReportViewModel : CommonRepositoryViewModel<ReportRepository>() {


    val mToolbarVMMyResume = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.resume_attachment.getResString(),
        background = Color.TRANSPARENT,
    )

    val bannerList = ObservableField<List<Int>>()

    val resumeData = MutableLiveData<ResumeData?>()


    val resumeDetectionData = MutableLiveData<ResumeData>()

    val selectIndex = MutableLiveData<Int>(0)

    override fun onCreate() {
        super.onCreate()
        initBannerData()
    }


    fun queryResumeList() {
        launchWithLoading(true, block = {
            response(ApiFactory.apiService.resumeQuery(PagedBody(1, 100)), result = {
                 if(this.data?.items.isNullOrEmpty()){
                     resumeData.postValue(null)
                 }else{
                     resumeData.postValue(this.data?.items?.first())
                 }

            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }


    fun queryLatestResume() {
        launchWithLoading(true, block = {
            response(mRepo.queryLatestResume(), result = {
                resumeData.postValue(this.data)
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }


    fun resumeMaterialQuality(resumeFileUrl: String, resumeFileName: String) {
        launch(block = {
            response(
                mRepo.api.resumeMaterialQuality(
                    ResumeMaterialQualityBody(
                        resumeFileUrl = resumeFileUrl,
                        resumeFileName = resumeFileName
                    )
                ), result = {
                    resumeDetectionData.postValue(this.data)
                }, error = {
                    val errorData = resumeDetectionData.value?.copy(
                        blockingIssuesMessage = "简历解析失败",
                        checkStatus = FAILED
                    )
                    errorData?.let {
                        resumeDetectionData.postValue(it)
                    }
                    this?.msg?.showToast()
                })
        }, onError = {
            val errorData = resumeData.value?.copy(
                blockingIssuesMessage = "简历解析失败",
                checkStatus = FAILED
            )
            errorData?.let { it ->
                resumeData.postValue(it)
            }
            it.message.toString().showToast()
        })
    }


    fun resumeMaterialQualityr() {
        launch(block = {
            response(
                mRepo.api.resumeMaterialQualityResult(
                    ResumeMaterialQualityBody(
                        checkId = resumeDetectionData.value?.checkId
                    )
                ), result = {
                    resumeDetectionData.postValue(this.data)
                }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })
    }


    private fun initBannerData() {
        val fakeBannerImages = listOf(
            R.drawable.img_home_banner1,
            R.drawable.img_home_banner2,
            R.drawable.img_home_banner3,
            R.drawable.img_home_banner4,
        )
        bannerList.set(fakeBannerImages)
    }
}
