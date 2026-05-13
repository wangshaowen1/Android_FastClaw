package com.shiyuan.fastclaw.feature.report.report

import android.net.Uri
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.common.ext.cleanupTempFile
import com.shiyuan.fastclaw.common.ext.isNetworkPath
import com.shiyuan.fastclaw.common.ext.toOssFilePath
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.loginToast
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.report.data.ReportRepository
import com.shiyuan.fastclaw.kit.SignUtils
import okio.Path

/**
 * @Introduce: 历史报告列表 ViewModel
 */
class HomeReportViewModel : AppRepositoryViewModel<ReportRepository>() {

    val bannerList = ObservableField<List<Int>>()

    val resumeData = MutableLiveData<ResumeData>()

    val selectIndex = MutableLiveData<Int>(0)

    override fun onCreate() {
        super.onCreate()
        initBannerData()
    }


    fun queryLatestResume() {
        launchWithLoading(true, block = {
            response(mRepo.queryLatestResume(), result = {
                resumeData.postValue(this.data)
            }, error = { this?.msg?.showToast() })
        }, onError = { it.message.toString().showToast() })

    }



    fun submitResume(uri: Uri, callback: (path: String) -> Unit) {
        setLoading(true)
        val context = App.instance
        val filePath = uri.toOssFilePath(context)
        if (filePath == null) {
            setLoading(false)
            "文件读取失败".showToast()
            return
        }

        val ossClient = OssClient.newInstance(context)
        ossClient.uploadFile(filePath, OssClient.TYPE_NORMAL, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val images = uploadSuccess.filterNotNull()
                if (images.isEmpty()) {
                    onUploadFail(null)
                } else {
                    Log.d("onUploadSuccess", images[0])
                    // 仅清理本地临时文件
                    if (!uri.isNetworkPath()) {
                        filePath.cleanupTempFile()
                    }
                    callback.invoke(images[0])
                    setLoading(false)
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                Log.d("onUploadFail", uploadFail?.get(0).toString())
                showToast(R.string.upload_failed)
                setLoading(false)
            }
        })
    }

    private fun initBannerData() {
        val fakeBannerImages = listOf(
            R.mipmap.img_banner_test,
            R.mipmap.img_banner_test,
            R.mipmap.img_banner_test,
            R.mipmap.img_banner_test
        )
        bannerList.set(fakeBannerImages)
    }
}
