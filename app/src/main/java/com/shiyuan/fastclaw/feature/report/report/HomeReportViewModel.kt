package com.shiyuan.fastclaw.feature.report.report

import UnderstandingScore
import android.graphics.Color
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.parseMessage
import com.shiyuan.fastclaw.core.ext.request
import com.shiyuan.fastclaw.core.ext.requestWithLoading
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.AttributeConfirmBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeMaterialQualityBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeUploadSubmitMyBody
import com.shiyuan.fastclaw.core.network.apis.response.ResumeListData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeUploadSubmitData
import com.shiyuan.fastclaw.core.network.apis.response.ResumeUploadSubmitMyData
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.report.data.ReportRepository
import com.shiyuan.fastclaw.feature.report.genner.GenerateReportActivity.Companion.FAILED
import java.util.UUID

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


    val resumeListData = MutableLiveData<ResumeListData?>()

    val resumeUploadResult = MutableLiveData<ResumeUploadSubmitMyData?>()

    val selectIndex = MutableLiveData<Int>(0)

    override fun onCreate() {
        super.onCreate()
        initBannerData()
    }


    fun queryResumeList() {
        launchWithLoading(true, block = {
            response(ApiFactory.apiService.resumeQuery(PagedBody(1, 100)), result = {
                if (this.data?.items.isNullOrEmpty()) {
                    resumeListData.postValue(null)
                } else {
                    resumeListData.postValue(this.data?.items?.first())
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

/*
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
    }*/


    private fun initBannerData() {
        val fakeBannerImages = listOf(
            R.drawable.img_home_banner1,
            R.drawable.img_home_banner2,
            R.drawable.img_home_banner3,
            R.drawable.img_home_banner4,
        )
        bannerList.set(fakeBannerImages)
    }

    /**
     * 提交简历上传结果
     * @param resumeFileUrl 简历文件地址或 OSS key
     */
/*    fun submitResumeUpload( resumeFileUrl: String?,fileName: String?, replaceResumeTaskId: String?, replaceResumeId: String?) {
        launchWithLoading(true, block = {
            response(  mRepo.api.submitResumeMyUpload(
                ResumeUploadSubmitMyBody(
                    replaceResumeTaskId = replaceResumeTaskId,
                    fileName = fileName,
                    replaceResumeId = replaceResumeId,
                    clientUploadId = UUID.randomUUID().toString(),
                    resumeFileUrl = resumeFileUrl,
                )
            ), result = {
                if (this.data?.accepted == true) {
                    resumeUploadResult.postValue(this.data)
                    "上传成功".showToast()
                } else {
                    "上传失败".showToast()
                }
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }*/

    fun submitResumeUpload( resumeFileUrl: String?,fileName: String?, replaceResumeTaskId: String?) {
        requestWithLoading(
            apiBlock = {
                mRepo.api.submitResumeMyUpload(
                    ResumeUploadSubmitMyBody(
                        replaceResumeTaskId = replaceResumeTaskId,
                        fileName = fileName,
                        resumeFileUrl = resumeFileUrl,
                    )
                )
            },
            showLoading = true,
            showToastError = true,
        ) {
            if (this.data?.accepted == true) {
                resumeUploadResult.postValue(this.data)
                "上传成功".showToast()
            } else {
                "上传失败".showToast()
            }
        }
    }

}
