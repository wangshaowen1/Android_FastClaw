package com.shiyuan.fastclaw.feature.report.report

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.AppRepositoryViewModel
import com.shiyuan.fastclaw.core.AppRepository

/**
 * @Introduce: 历史报告列表 ViewModel
 */
class HomeReportViewModel : AppRepositoryViewModel<AppRepository>() {

    val bannerList = ObservableField<List<Int>>()

    val selectIndex = MutableLiveData<Int>(0)

    override fun onCreate() {
        super.onCreate()
        initBannerData()
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
