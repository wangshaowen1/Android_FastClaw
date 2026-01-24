package com.kgmedia.fastfilm.feature.web

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.feature.web.data.BrowserRepository
import com.kuaige.payment.PaymentClient
import com.nhsd.basic.ext.getDrawable
import com.nhsd.basic.ext.getResColor

class BrowserViewModel : AppRepositoryViewModel<BrowserRepository>() {

    val title by lazy { mBundle.getString(Constants.Extra.TITLE) }
    val url by lazy { mBundle.getString(Constants.Extra.URL) }
    val interceptBack by lazy { mBundle.getBoolean(Constants.Extra.INTERCEPT_BACK) }
    val darkStyle by lazy { mBundle.getBoolean(Constants.Extra.DARK_STYLE) }
    val isLoading = MutableLiveData(false)
    val isLoadError = MutableLiveData(false)

    lateinit var paymentClient: PaymentClient
    var orderNo: String? = null

    val mToolbarVM by lazy {
        ToolbarViewModel(
            navigationAction = { back() },
            navigationDrawable = if (darkStyle) R.drawable.ic_nav_back.getDrawable() else R.drawable.ic_nav_back_black.getDrawable(),
            title = title,
            titleColor = if (darkStyle) (R.color.textColorPrimary.getResColor()) else R.color.color_121212.getResColor(),
            background = if (darkStyle) Color.TRANSPARENT else Color.WHITE
        )
    }
    val openWebUrl = MutableLiveData<String?>(null)
}