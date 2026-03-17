package com.kgmedia.fastfilm.feature.splash.newer

import androidx.databinding.ObservableBoolean
import androidx.media3.common.Player
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.network.apis.entity.ExampleWorkInfo
import com.kgmedia.fastfilm.feature.payment.pay.PaymentViewModel
import com.kgmedia.fastfilm.kit.media.PlayerFactory
import com.kgmedia.fastfilm.kit.media.play
import com.nhsd.basic.ext.getParcelableValue

class FirstRechargeViewModel : PaymentViewModel<AppRepository>() {

    val example by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, ExampleWorkInfo::class.java) }



    fun wxPay(){

    }

}