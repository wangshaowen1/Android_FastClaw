package com.kgmedia.fastfilm.feature.play

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.feature.play.UnlockSeriesShortDialog.Companion.LOCKED
import com.kgmedia.fastfilm.feature.play.UnlockSeriesShortDialog.Companion.UNLOCK_COINS
import com.kgmedia.fastfilm.feature.play.data.ShortPlayRepository

class UnLockSeriesShortViewModel : AppRepositoryViewModel<ShortPlayRepository>() {

    val position by lazy { mBundle.getString(Constants.Extra.POSITION) }
    val locked by lazy { mBundle.getBoolean(LOCKED) }
    val unlockCoins by lazy { mBundle.getInt(UNLOCK_COINS) }
    val shortLocked by lazy { ObservableBoolean(false) }
    override fun onCreate() {
        super.onCreate()
        shortLocked.set(locked)
    }
}