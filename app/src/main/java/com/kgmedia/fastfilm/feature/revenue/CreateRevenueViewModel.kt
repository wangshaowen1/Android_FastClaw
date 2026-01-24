package com.kgmedia.fastfilm.feature.revenue

import android.graphics.Color
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.ProfitDetailInfo
import com.kgmedia.fastfilm.core.network.apis.entity.UserRevenueInformationInfo
import com.kgmedia.fastfilm.core.network.apis.request.UserTokenBody
import com.kgmedia.fastfilm.core.response
import kotlinx.coroutines.flow.Flow

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:创造收益
 * @Data: 2025/11/4/周二  11:02
 * @Version: 1.0
 * *****************************************
 */
class CreateRevenueViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        title = "创作收益",
        navigationAction = { back() },
        background = Color.TRANSPARENT,
    )

    //用户收益信息
    val mUserRevenueInformation = ObservableField<UserRevenueInformationInfo>()

    //累计提现
    val mTotalWithdrawal = ObservableField<String>()

    //收益金额
    val mIncome = ObservableField<String>()

    //收益比例说明
    val mIncomeRatio = ObservableField<String>()

    override fun onCreate() {
        super.onCreate()
        userRevenueInformation()
    }

    fun createRevenue(): Flow<PagingData<ProfitDetailInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = Constant.Paged.PAGE_SIZE,
                pageSize = Constant.Paged.PAGE_SIZE,
                prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
            ),
            pagingSourceFactory = {
                CreateRevenuePagingSource()
            }).flow.cachedIn(viewModelScope)
    }

    /**
     * 用户收益信息
     */
    fun userRevenueInformation() {
        launch {
            response(
                bean = mRepo.api.userRevenueInformation(UserTokenBody()),
                result = {
                    mUserRevenueInformation.set(data)
                    data?.apply {
                        if ((profitRatio ?: 0) > 0) {
                            mIncomeRatio.set("""<span>当前每章作品的创作收益比例为<span style="color:#FF4B00;">${profitRatio}%</span></span>""")
                        } else {
                            mIncomeRatio.set(null)
                        }
                        mTotalWithdrawal.set(totalWithdrawAmount?.formatMoney())
                        mIncome.set(totalProfitAmount?.formatMoney())
                    }
                })
        }
    }
}