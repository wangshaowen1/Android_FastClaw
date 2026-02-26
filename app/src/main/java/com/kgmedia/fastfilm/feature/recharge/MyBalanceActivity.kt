package com.kgmedia.fastfilm.feature.recharge

import android.os.Bundle
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.ext.setupCommonBanner
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.entity.BannerInfo
import com.kgmedia.fastfilm.core.network.apis.request.BannerBody
import com.kgmedia.fastfilm.databinding.ActivityMyBalanceBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.startActivity
import com.zhpan.bannerview.BannerViewPager

class MyBalanceActivity : BaseViewModelActivity<ActivityMyBalanceBinding, MyBalanceViewModel>() {
    companion object {
        fun start() {
            topActivity?.startActivity(MyBalanceActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeEvents {
            on<Events.UserDataEvent> {
                mRealVM.updateBalance()
            }
        }
        setupCommonBanner(binding.banner.root, binding.banner.banner as BannerViewPager<BannerInfo>) {
            ApiFactory.apiService.getAppBanner(BannerBody(BannerBody.LOCATION_HOME)).data
        }
    }

    override fun createViewModel(): MyBalanceViewModel {
        return MyBalanceViewModel(R.layout.item_recharge_payment)
    }
}