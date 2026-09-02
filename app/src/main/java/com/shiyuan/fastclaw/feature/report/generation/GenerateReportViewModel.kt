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
import com.shiyuan.fastclaw.core.ext.setBlackDialog
import com.shiyuan.fastclaw.core.network.apis.entity.ReportData
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.InterviewReportData
import com.shiyuan.fastclaw.core.network.apis.request.JdMaterialQualityBody
import com.shiyuan.fastclaw.core.network.apis.request.ResumeMaterialQualityBody
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.report.generation.GenerateReportActivity.Companion.FAILED

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


    fun interviewReportr() {
        launchWithLoading(false, block = {
            response(
                mRepo.api.interviewReportResult(
                    InterviewReportData(
                        reportId = reportData.value?.reportId
                    )
                ), result = {
                    reportData.postValue(this.data)
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

    fun jdMaterialQualityr(type: String) {
        launch(block = {
            val body = if (type == IMAGE) {
                JdMaterialQualityBody(checkId = jdImageData.value?.checkId)
            } else {
                JdMaterialQualityBody(checkId = jdTextData.value?.checkId)
            }
            response(
                mRepo.api.jdMaterialQualityResult(
                    body
                ), result = {
                    if (type == IMAGE) {
                        jdImageData.postValue(this.data)
                    } else {
                        jdTextData.postValue(this.data)
                    }
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



    fun resumeMaterialQualityr() {
        launch(block = {
            response(
                mRepo.api.resumeMaterialQualityResult(
                    ResumeMaterialQualityBody(
                        checkId = resumeData.value?.checkId
                    )
                ), result = {
                    resumeData.postValue(this.data)
                }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })
    }


}
