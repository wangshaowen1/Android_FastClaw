package com.kgmedia.fastfilm.feature.payment.points

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.rv.InfiniteQuickAdapter
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.request.DanmakuBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorRepository
import com.kgmedia.fastfilm.feature.payment.pay.PaymentViewModel
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast

/**
 * 解锁首次作品弹窗
 */
class FirstTimeUnlockViewModel : PaymentViewModel<ShortDramaEditorRepository>() {
    private val dramasId by lazy { mBundle.getString(Constants.Extra.DRAMAS_ID) }
    val loading by lazy { ObservableBoolean() }
    val sampleAdapter by lazy { InfiniteQuickAdapter<ItemFirstTimeRechargeSampleVM>(R.layout.item_first_time_recharge_sample, emptyList()) }
    val cost by lazy { ObservableField<CharSequence>() }

    override fun onCreate() {
        super.onCreate()
        getDanmakuList()
        val points = mBundle.getInt(Constants.Extra.COST)
        if (points > 0) {
            applyCost(points)
        } else {
            calcCost()
        }
    }

    private fun getDanmakuList() {
        launch {
            response(mRepo.api.danmakuList(DanmakuBody(1, 1, 100))) {
                data?.items?.map { ItemFirstTimeRechargeSampleVM(it) }?.let { sampleAdapter.submitList(it) }
            }
        }
    }

    private fun calcCost() {
        launch {
            response(mRepo.calculatePrice(dramasId)) {
                data?.pointAmount?.let { applyCost(it) }
            }
        }
    }

    private fun applyCost(points: Int) {
        cost.set("解锁后续图片还需${points}积分")
    }

    fun unlock() {
        loading.set(true)
        launch(
            block = {
                response(
                    mRepo.unlockFirstGeneratedShortDrama(dramasId),
                    result = {
                        loading.set(false)
                        "解锁成功".showToast()
                        dismiss()
                        Global.refreshUserInfo()
                        FlowEventBus.postSync(Events.ShortDramasUpdatedEvent(dramasId))
                    },
                    error = {
                        loading.set(false)
                    })
            },
            onError = {
                loading.set(false)
            }
        )
    }

}