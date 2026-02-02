package com.kgmedia.fastfilm.feature.series.setprice

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.KeyboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.common.ext.getChildByType
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.core.network.apis.entity.SeriesPriceConfigInfo
import com.kgmedia.fastfilm.core.network.apis.request.SeriesSetPriceBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.payment.vip.MembershipBenefitsActivity
import com.kgmedia.fastfilm.feature.series.SeriesRepository
import com.kgmedia.fastfilm.widget.LoadStateViewHolder
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast

/**
 * 剧集-设置价格
 */
class SetPriceViewModel : AppRepositoryViewModel<SeriesRepository>() {
    private val seriesId by lazy { mBundle.getString(Constants.Extra.ID) }
    val release by lazy { mBundle.getBoolean(SetPriceDialog.EXTRA_RELEASE) }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val config by lazy { ObservableField<SeriesPriceConfigInfo>() }
    val priceAdapter by lazy { QuickAdapter<ItemSetPriceVM>(R.layout.item_set_price, emptyList()) }
    var selectedItem: ItemSetPriceVM? = null
    val startEpisode by lazy { MutableLiveData("4") }

    //当前用户是否是会员
    val isVip = ObservableField<Boolean>()

    //会员比例说明
    val vipRatioDesc = ObservableField<String>()

    override fun onCreate() {
        super.onCreate()
        EventManager.onAppTrack(
            AppEvent.JG_VIEW_SET_UP_CHARGES_SHEET,
            mapOf(GrowingIO.KEY_ALL_GENERAL_ID to seriesId)
        )
        //会员信息更新
        memberInformationUpdate()
        getPriceConfig()
    }

    override fun onResume() {
        super.onResume()
        //会员信息更新
        memberInformationUpdate()
    }

    private fun memberInformationUpdate() {
//        isVip.set(User.get().isVip)
//        if (isVip.get() == true) {
//            vipRatioDesc.set("")
//            return
//        }
//        //1-初级会员 2-中级会员 3-高级会员
//        val vipName = when (User.get().vipType) {
//            User.VIP_TYPE_PRIMARY -> "初级会员"
//            User.VIP_TYPE_MIDDLE -> "中级会员"
//            User.VIP_TYPE_LIFE -> "高级会员"
//            else -> ""
//        }
//        vipRatioDesc.set("当前为${vipName}，分成比例${User.get().profitSharingRatio}%")
    }

    fun getPriceConfig() {
        loadState.set(LoadStateViewHolder.LOADING)
        if (config.get() == null) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                mRepo.seriesPriceConfig(seriesId).data?.apply {
                    config.set(this)
                    startEpisode.value = (freeEndWorkNo ?: 0).coerceAtLeast(4).toString()
                    defaultPriceList?.map { price ->
                        ItemSetPriceVM(price).apply {
                            onItemClick.value = { onItemClick(it, this) }
                        }
                    }.let { priceAdapter.submitList(it) }
                    val curPrice = curPrice ?: 0
                    //是否自定义=当前价格大于0且不在默认价格集合中
                    val customized =
                        curPrice > 0 && defaultPriceList?.any { it == curPrice } != true
                    priceAdapter.add(
                        ItemSetPriceVM(
                            if (customized) curPrice else 0,
                            customize = true
                        ).apply { onItemClick.value = { onItemClick(it, this) } })
                    priceAdapter.add(ItemSetPriceVM(0, free = true).apply {
                        onItemClick.value = { onItemClick(it, this) }
                    })
                    if (freeStatus == 1) {
                        priceAdapter.items.firstOrNull { it.free }?.let { onItemClick(null, it) }
                    } else if (customized) {
                        priceAdapter.items.firstOrNull { it.customize }
                            ?.let { onItemClick(null, it) }
                    } else if (curPrice > 0) {
                        priceAdapter.items.firstOrNull { it.data.get() == curPrice }
                            ?.let { onItemClick(null, it) }
                    }
                }
                updateLoadState()
            },
            onError = {
                updateLoadState()
            }
        )
    }

    private fun updateLoadState() {
        loadState.set(LoadStateViewHolder.CONTENT)
        if (config.get() == null) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    private fun onItemClick(view: View?, item: ItemSetPriceVM) {
        selectedItem?.selected?.set(false)
        item.selected.set(true)
        selectedItem = item
        if (item.customize) {
            (view as? ViewGroup)?.getChildByType(
                EditText::class.java,
                AppCompatEditText::class.java
            )?.let {
                KeyboardUtils.showSoftInput(it)
                it.requestFocus()
                (it as? EditText)?.setSelection(it.length())
            }
        } else {
            view?.let { KeyboardUtils.hideSoftInput(it) }
        }
    }

    fun confirm() {
        val selectedItem = selectedItem
        if (selectedItem == null) {
            "请设置价格".showToast()
            return
        }
        var unlockPrice = 0L
        var freeStatus = 0
        if (selectedItem.free) {
            freeStatus = 1
        } else if (selectedItem.customize) {
            val customPrice = ((selectedItem.input.value?.toFloatOrNull() ?: 0f) * 1000).toLong()
            if (customPrice < 1) {
                "请设置价格".showToast()
                return
            }
            unlockPrice = customPrice
        } else {
            unlockPrice = selectedItem.data.get() ?: 0L
        }
        val startEpisode = startEpisode.value
        if (startEpisode.isNullOrEmpty()) {
            "请输入从第几集开始收费".showToast()
            return
        }
        val startChargeWorkNo = startEpisode.toIntOrNull() ?: 0
        if (startChargeWorkNo < 4) {
            "从第4章才能开始收费哦~".showToast()
            return
        }
        val body = SeriesSetPriceBody(
            dramasSeriesId = seriesId,
            unlockPrice = unlockPrice,
            startChargeWorkNo = startChargeWorkNo,
            freeStatus = freeStatus
        )
        launchWithLoading {
            if (release) {
                response(
                    bean = mRepo.seriesPublish(body),
                    result = {
                        "上架成功，你的作品可以被看到了哦~".showToast()
                        FlowEventBus.postSync(Events.SeriesUpdatedEvent(seriesId))
                        dismiss()
                    },
                    error = {
                        this?.msg?.showToast()
                        //40013,"会员才可以操作"
                        if (this?.errorCode == 40013) {
                            MembershipBenefitsActivity.start(withToast = false)
                        }
                    })
            } else {
                response(
                    bean = mRepo.seriesSetPrice(body),
                    result = {
                        "设置成功".showToast()
                        FlowEventBus.postSync(Events.SeriesUpdatedEvent(seriesId))
                        dismiss()
                    })
            }
        }
    }

    fun startVip() {
        MembershipBenefitsActivity.start(withToast = false)
    }
}