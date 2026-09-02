package com.shiyuan.fastclaw.feature.shrimp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.ObservableField
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.gyf.immersionbar.ImmersionBar
import com.kuaige.payment.PaymentClient
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.core.network.apis.entity.PointConfigInfo
import com.shiyuan.fastclaw.core.network.apis.entity.TradePayWayInfo
import com.shiyuan.fastclaw.databinding.ActivityMineShrimpRechargeBinding
import com.shiyuan.fastclaw.feature.mine.MineViewModel
import com.shiyuan.fastclaw.feature.payment.ItemPaymentMethodVm
import com.shiyuan.fastclaw.feature.payment.PaymentMethod
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.decoration.GridSpacingItemDecoration
import com.shiyuan.fastclaw.feature.order.MyOrderActivity

/**
 * @Introduce: 虾条充值
 */
class MineShrimpRechargeActivity :
    BaseViewModelActivity<ActivityMineShrimpRechargeBinding, MineViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            activity.startActivity(Intent(activity, MineShrimpRechargeActivity::class.java))
        }
    }

    private var selectedPackageIndex = 1
    private var selectedPaymentIndex = -1
    private var payWayList: List<TradePayWayInfo> = emptyList()
    private val packageAdapter by lazy {
        QuickAdapter<ItemMineRechargePackageVM>(R.layout.item_mine_recharge_package, emptyList())
    }
    private val paymentAdapter by lazy {
        QuickAdapter<ItemPaymentMethodVm>(R.layout.item_payment_method_points_recharge, emptyList())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObservers()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_f5f6fa)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        mRealVM.onMenuClick = {
            //MyShrimpChipsActivity.start(this)
            MyOrderActivity.start(this,0)
        }

        binding.rvRechargePackage.apply {
            layoutManager = GridLayoutManager(this@MineShrimpRechargeActivity, 2)
            if (itemDecorationCount == 0) {
                addItemDecoration(GridSpacingItemDecoration(2, 12.dp, 12.dp))
            }
            isNestedScrollingEnabled = false
            adapter = packageAdapter
        }
        binding.rvPaymentMethod.apply {
            layoutManager = LinearLayoutManager(this@MineShrimpRechargeActivity)
            isNestedScrollingEnabled = false
            adapter = paymentAdapter
        }

        binding.btnRecharge.setOnSingleClickListener {
            if (AppNavigator.checkLoginIntercepted(false)) {
                return@setOnSingleClickListener
            }
            val rechargePackage = packageAdapter.items
                .getOrNull(selectedPackageIndex)
                ?.data
                ?: return@setOnSingleClickListener
            val goodsCode = rechargePackage.get()?.goodsCode ?: return@setOnSingleClickListener
            val payment = selectedPayWay()?.toPaymentMethod()
            if (payment == null) {
                R.string.select_the_payment_method.getResString().showToast()
                return@setOnSingleClickListener
            }
            mRealVM.rechargeShrimp(payment, goodsCode)
        }
        binding.tvRechargeAgreement.setOnSingleClickListener {
            SimpleBrowserActivity.launch(this, Constants.SHRIMPRECHARGEAGREEMENT_URL)
        }

        mRealVM.getRechargePackages()
        mRealVM.getPayWays()
    }

    private fun initObservers() {
        mRealVM.userPoints.observe(this) { points ->
            binding.tvAvailableSticks.text = points?.takeIf { it.isNotBlank() } ?: "0"
        }
        mRealVM.rechargePackages.observe(this) { packages ->
            submitRechargePackages(packages.orEmpty())
        }
        mRealVM.payWays.observe(this) { payWays ->
            submitPayWays(payWays.orEmpty())
        }
    }

    private fun submitRechargePackages(packages: List<PointConfigInfo>) {
        val packageData = packages.mapNotNull { it.toRechargePackage() }
        selectedPackageIndex = packageData.indexOfFirst { it.isDefault }.takeIf { it >= 0 }
            ?: packageData.indices.firstOrNull()
            ?: -1
        val packageItems = packageData.mapIndexed { index, item ->
            ItemMineRechargePackageVM(item).apply {
                select.set(index == selectedPackageIndex)
                onItemClick.value = {
                    updatePackageSelected(index)
                }
            }
        }
        packageAdapter.submitList(packageItems)
    }

    private fun updatePackageSelected(index: Int) {
        selectedPackageIndex = index
        packageAdapter.items.forEachIndexed { itemIndex, item ->
            item.select.set(itemIndex == index)
        }
    }

    private fun updatePaymentSelected(index: Int) {
        selectedPaymentIndex = index
        paymentAdapter.items.forEachIndexed { itemIndex, item ->
            item.select.set(itemIndex == index)
        }
    }

    private fun submitPayWays(payWays: List<TradePayWayInfo>) {
        payWayList = payWays.filter { it.toPaymentMethod() != null }.sortedWith(
            compareByDescending<TradePayWayInfo> { it.isDefault == 1 }
                .thenByDescending { it.sortNo ?: Int.MIN_VALUE }
        )
        selectedPaymentIndex = payWayList.indexOfFirst { it.isDefault == 1 }.takeIf { it >= 0 }
            ?: payWayList.indices.firstOrNull()
            ?: -1
        val paymentItems = payWayList.mapIndexed { index, item ->
            ItemPaymentMethodVm(item.toPaymentMethodItem()).apply {
                select.set(index == selectedPaymentIndex)
                onItemClick.value = {
                    updatePaymentSelected(index)
                }
            }
        }
        paymentAdapter.submitList(paymentItems)
    }

    private fun selectedPayWay(): TradePayWayInfo? {
        return payWayList.getOrNull(selectedPaymentIndex)
    }
}

private fun TradePayWayInfo.toPaymentMethodItem(): PaymentMethod {
    val method = toPaymentMethod() ?: PaymentClient.PaymentMethod.WxPay
    return PaymentMethod(
        name = displayName(),
        icon = if (method == PaymentClient.PaymentMethod.Alipay) {
            R.drawable.ic_pm_alipay
        } else {
            R.drawable.ic_pm_wechat
        },
        type = method
    )
}

private fun TradePayWayInfo.toPaymentMethod(): PaymentClient.PaymentMethod? {
    return when (payWay) {
        TradePayWayInfo.PAY_WAY_ALIPAY -> PaymentClient.PaymentMethod.Alipay
        TradePayWayInfo.PAY_WAY_WECHAT,
        TradePayWayInfo.PAY_WAY_BAOFU -> PaymentClient.PaymentMethod.WxPay
        else -> null
    }
}

private fun TradePayWayInfo.displayName(): String {
    val defaultNameRes = if (payWay == TradePayWayInfo.PAY_WAY_ALIPAY) {
        R.string.alipay_payment
    } else {
        R.string.wechat_pay
    }
    return payWayName?.takeIf { it.isNotBlank() } ?: defaultNameRes.getResString()
}

private fun PointConfigInfo.toRechargePackage(): MineRechargePackage? {
    val amount = pointNum?.toString()?.takeIf { it.isNotBlank() }
        ?: pointName?.takeIf { it.isNotBlank() }
        ?: goodsName?.takeIf { it.isNotBlank() }
        ?: return null
    val price = priceText?.takeIf { it.isNotBlank() }
        ?: goodsPrice?.takeIf { it.isNotBlank() }?.let {
            if (it.startsWith("¥")) it else "¥${it.removeSuffix("元")}"
        }.orEmpty()
    return MineRechargePackage(
        amount = amount,
        price = price,
        goodsCode = goodsCode,
        isDefault = isDefault == 1
    )
}

data class MineRechargePackage(
    val amount: String,
    val price: String,
    val goodsCode: Long?,
    val isDefault: Boolean = false
)

class ItemMineRechargePackageVM(data: MineRechargePackage?) :
    AppItemViewModel<MineRechargePackage>(data) {
    val select = ObservableField(false)
}
