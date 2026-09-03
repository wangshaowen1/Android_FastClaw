package com.shiyuan.fastclaw.feature.report.generation

import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.base.viewmodel.ToolbarViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.common.Constants.Extra.IMAGE
import com.shiyuan.fastclaw.common.ext.cleanupTempFile
import com.shiyuan.fastclaw.common.ext.isNetworkPath
import com.shiyuan.fastclaw.common.ext.toOssFilePath
import com.shiyuan.fastclaw.core.CommonRepositoryViewModel
import com.shiyuan.fastclaw.core.ext.launch
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.launchPolling
import com.shiyuan.fastclaw.core.ext.PollingState
import com.shiyuan.fastclaw.core.ext.setBlackDialog
import com.shiyuan.fastclaw.core.network.apis.entity.ReportData
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.InterviewReportData
import com.shiyuan.fastclaw.core.network.apis.request.JdMaterialQualityBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeMaterialQualityBody
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.report.generation.GenerateReportActivity.Companion.FAILED
import com.shiyuan.fastclaw.feature.report.generation.GenerateReportActivity.Companion.PENDING
import com.shiyuan.fastclaw.feature.report.generation.GenerateReportActivity.Companion.RUNNING
import kotlinx.coroutines.Job

/**
 * @Introduce: 历史报告列表 ViewModel
 */
class GenerateReportViewModel : CommonRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.generate_report.getResString(),
        background = Color.TRANSPARENT,
    )


    val mToolbarVM2 = ToolbarViewModel(
        navigationAction = { finish() },
        title = "",
        background = Color.TRANSPARENT,
    )
    val resumeData = MutableLiveData<ResumeData>()

    val isJd = MutableLiveData<Boolean>()

    val jdTextData = MutableLiveData<ResumeData>()
    val jdImageData = MutableLiveData<ResumeData>()
    val reportData = MutableLiveData<ReportData>()


    val photoSize = MutableLiveData<Int>(0)

    val tableSelect = MutableLiveData<Int>(0)

    private var resumePollingJob: Job? = null
    private var jdPollingJob: Job? = null
    private var jdPollingType: String? = null
    private var reportPollingJob: Job? = null

    fun startResumePolling() {
        if (resumePollingJob?.isActive == true) return

        resumePollingJob = launchPolling(
            intervalMs = POLLING_INTERVAL_MS,
            retryOnNetworkError = true,
            apiBlock = {
                mRepo.api.resumeMaterialQualityResult(
                    ResumeMaterialQualityBody(checkId = resumeData.value?.checkId)
                )
            },
            checkState = { response ->
                when {
                    response?.isSuccess != true -> PollingState.FAIL
                    response.data == null -> PollingState.CONTINUE
                    response.data?.safeCheckStatus == RUNNING || response.data?.safeCheckStatus == PENDING -> {
                        PollingState.CONTINUE
                    }
                    else -> PollingState.SUCCESS
                }
            },
            onSuccess = { response ->
                resumePollingJob = null
                response?.data?.let { resumeData.postValue(it) }
            },
            onFail = { apiResponse, _ ->
                resumePollingJob = null
                response(
                    apiResponse,
                    result = {},
                    error = { this?.msg?.showToast() },
                )
            },
            onPolling = { response ->
                response?.data?.let { resumeData.postValue(it) }
            },
        )
    }

    fun stopResumePolling() {
        resumePollingJob?.cancel()
        resumePollingJob = null
    }

    fun startJdPolling(type: String) {
        if (jdPollingJob?.isActive == true) {
            if (jdPollingType == type) return
            stopJdPolling()
        }
        jdPollingType = type

        jdPollingJob = launchPolling(
            intervalMs = POLLING_INTERVAL_MS,
            retryOnNetworkError = true,
            apiBlock = {
                val checkId = if (type == IMAGE) {
                    jdImageData.value?.checkId
                } else {
                    jdTextData.value?.checkId
                }
                mRepo.api.jdMaterialQualityResult(JdMaterialQualityBody(checkId = checkId))
            },
            checkState = { response ->
                when {
                    response?.isSuccess != true -> PollingState.FAIL
                    response.data == null -> PollingState.CONTINUE
                    response.data?.safeCheckStatus == RUNNING || response.data?.safeCheckStatus == PENDING -> {
                        PollingState.CONTINUE
                    }
                    else -> PollingState.SUCCESS
                }
            },
            onSuccess = { response ->
                jdPollingJob = null
                jdPollingType = null
                response?.data?.let { data ->
                    if (type == IMAGE) {
                        jdImageData.postValue(data)
                    } else {
                        jdTextData.postValue(data)
                    }
                }
            },
            onFail = { apiResponse, _ ->
                jdPollingJob = null
                jdPollingType = null
                setLoading(false)
                response(
                    apiResponse,
                    result = {},
                    error = { this?.msg?.showToast() },
                )
            },
            onPolling = { response ->
                response?.data?.let { data ->
                    if (type == IMAGE) {
                        jdImageData.postValue(data)
                    } else {
                        jdTextData.postValue(data)
                    }
                }
            },
        )
    }

    fun stopJdPolling() {
        jdPollingJob?.cancel()
        jdPollingJob = null
        jdPollingType = null
    }

    fun startReportPolling() {
        if (reportPollingJob?.isActive == true) return

        reportPollingJob = launchPolling(
            intervalMs = POLLING_INTERVAL_MS,
            retryOnNetworkError = true,
            apiBlock = {
                mRepo.api.interviewReportResult(
                    InterviewReportData(reportId = reportData.value?.reportId)
                )
            },
            checkState = { response ->
                when {
                    response?.isSuccess != true -> PollingState.FAIL
                    response.data == null -> PollingState.CONTINUE
                    response.data?.reportStatus in REPORT_TERMINAL_STATUSES -> PollingState.SUCCESS
                    else -> PollingState.CONTINUE
                }
            },
            onSuccess = { response ->
                reportPollingJob = null
                response?.data?.let { reportData.postValue(it) }
            },
            onFail = { apiResponse, _ ->
                reportPollingJob = null
                response(
                    apiResponse,
                    result = {},
                    error = { this?.msg?.showToast() },
                )
            },
            onPolling = { response ->
                response?.data?.let { reportData.postValue(it) }
            },
        )
    }

    fun stopReportPolling() {
        reportPollingJob?.cancel()
        reportPollingJob = null
    }


    fun interviewReport(data: InterviewReportData, callback: () -> Unit) {
        launchWithLoading(false, block = {
            response(
                mRepo.api.interviewReport(
                    data
                ), result = {
                    reportData.postValue(this.data)
                }, error = {
                    Log.d("interviewReport", this?.msg.toString())
                    setLoading(false)
                    callback.invoke()
                    this?.msg?.showToast()
                }
            )
        }, onError = {
            setLoading(false)
            it.message.toString().showToast()
        })
    }


    fun jdQualityQuality(jdImageUrls: List<String>?, jdText: String?, type: String) {
        setLoading(true)
        launch(block = {
            response(
                mRepo.api.jdMaterialQuality(
                    JdMaterialQualityBody(
                        jdInputType = type,
                        jdText = jdText,
                        jdImageUrls = jdImageUrls
                    )
                ), result = {
                    if (type == IMAGE) {
                        jdImageData.postValue(this.data)
                    } else {
                        jdTextData.postValue(this.data)
                    }
                }, error = {
                    setLoading(false)
                    this?.msg?.showToast()
                }
            )
        }, onError = {
            setLoading(false)
            it.message.toString().showToast()
        })

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
                    resumeData.postValue(this.data)
                }, error = {
                    val errorData = resumeData.value?.copy(
                        blockingIssuesMessage = "简历解析失败",
                        checkStatus = FAILED
                    )
                    errorData?.let {
                        resumeData.postValue(it)
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



    override fun onDestroy() {
        stopResumePolling()
        stopJdPolling()
        stopReportPolling()
        super.onDestroy()
    }

    private companion object {
        const val POLLING_INTERVAL_MS = 2000L
        val REPORT_TERMINAL_STATUSES = setOf("SUCCESS", "FAILED", "CANCELLED")
    }

}
