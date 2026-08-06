package com.shiyuan.fastclaw.feature.report.report

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
import com.shiyuan.fastclaw.core.network.apis.request.InterviewPassPackageResumeStatusBody
import com.shiyuan.fastclaw.core.network.apis.request.PagedBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeListDeleteBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeMaterialQualityBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeUploadSubmitMyBody
import com.shiyuan.fastclaw.core.network.apis.response.InterviewPassPackageResumeStatusData
import com.shiyuan.fastclaw.core.network.apis.response.JobAgentModuleData
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

    val resumeListData = MutableLiveData<List<ResumeListData>>(emptyList())

    val resumeUploadResult = MutableLiveData<ResumeUploadSubmitMyData?>()

    var code = MutableLiveData<String?>()

    val interviewPassPackageResumeStatus = MutableLiveData<InterviewPassPackageResumeStatusData?>()

    val jobAgentModuleList = MutableLiveData<List<JobAgentModuleData>>()

    val selectIndex = MutableLiveData<Int>(0)

    override fun onCreate() {
        super.onCreate()
        initBannerData()
    }

    fun queryCodeResumeBinding(code: String, onSuccess: (data: ResumeData?) -> Unit) {
        launch(block = {
            response(
                mRepo.api.queryCodeResumeBinding(mapOf("code" to code)),
                result = {
                    //data 可能为null
                    data?.let {
                        onSuccess(data)
                    }
                }, error = {

                })
        }, onComplete = {
            this.code.value = null
        })
    }

    fun queryResumeList() {
        launchWithLoading(false, block = {
            response(ApiFactory.apiService.resumeQuery(PagedBody(1, 100)), result = {
                resumeListData.postValue(this.data?.items.orEmpty())

            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }

    fun deleteResume(resumeTaskId: String?) {
        val taskId = resumeTaskId?.toLongOrNull() ?: return
        requestWithLoading(
            apiBlock = { mRepo.api.deleteResume(ResumeListDeleteBody(taskId)) },
            showLoading = true,
            showToastError = true,
        ) {
            queryResumeList()
        }
    }


    fun queryLatestResume() {
        launchWithLoading(true, block = {
            response(mRepo.queryLatestResume(), result = {
                resumeData.postValue(this.data)
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


    fun submitResumeUpload(
        resumeFileUrl: String?,
        fileName: String?,
        replaceResumeTaskId: String?,
        isShowLoading: Boolean = true,
        onComplete: () -> Unit? = {}
    ) {
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
            showLoading = isShowLoading,
            showToastError = true,
        ) {
            if (this.data?.accepted == true) {
                resumeUploadResult.postValue(this.data)
                "上传成功".showToast()
            } else {
                "上传失败".showToast()
            }
            onComplete.invoke()
        }
    }

    fun queryInterviewPassPackageResumeStatus() {
        requestWithLoading(
            apiBlock = {
                mRepo.api.queryInterviewPassPackageResumeStatus(
                    InterviewPassPackageResumeStatusBody()
                )
            },
            showLoading = true,
            showToastError = true,
        ) {
            interviewPassPackageResumeStatus.postValue(this.data)
        }
    }

    fun queryJobAgentModuleList() {
        request(
            apiBlock = { mRepo.api.queryJobAgentModuleList() },
            showToast = false,
        ) {
            jobAgentModuleList.postValue(this.data.orEmpty())
        }
    }

}
