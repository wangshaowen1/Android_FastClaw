package com.shiyuan.fastclaw.feature.report.report

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.ext.loginToast
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.response
import com.shiyuan.fastclaw.feature.report.data.ReportRepository
import com.shiyuan.fastclaw.kit.SignUtils

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
