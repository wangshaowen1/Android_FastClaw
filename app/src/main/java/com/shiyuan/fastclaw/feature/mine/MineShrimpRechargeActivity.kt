package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.ObservableField
import androidx.recyclerview.widget.GridLayoutManager
import com.gyf.immersionbar.ImmersionBar
import com.kuaige.payment.PaymentClient
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.core.network.apis.entity.PointConfigInfo
import com.shiyuan.fastclaw.databinding.ActivityMineShrimpRechargeBinding
import com.shiyuan.fastclaw.feature.mine.viewModel.MineViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.decoration.GridSpacingItemDecoration

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
    private var selectedWechat = true
    private val packageAdapter by lazy {
        QuickAdapter<ItemMineRechargePackageVM>(R.layout.item_mine_recharge_package, emptyList())
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
            MyShrimpChipsActivity.start(this)
        }

        binding.rvRechargePackage.apply {
            layoutManager = GridLayoutManager(this@MineShrimpRechargeActivity, 2)
            if (itemDecorationCount == 0) {
                addItemDecoration(GridSpacingItemDecoration(2, 12.dp, 12.dp))
            }
            isNestedScrollingEnabled = false
            adapter = packageAdapter
        }

        binding.llWechatPay.setOnSingleClickListener { updatePaymentSelected(true) }
        binding.llAlipay.setOnSingleClickListener { updatePaymentSelected(false) }

        binding.btnRecharge.setOnSingleClickListener {
            if (AppNavigator.checkLoginIntercepted(false)) {
                return@setOnSingleClickListener
            }
            val rechargePackage = packageAdapter.items
                .getOrNull(selectedPackageIndex)
                ?.data
                ?: return@setOnSingleClickListener
            val goodsCode = rechargePackage.get()?.goodsCode ?: return@setOnSingleClickListener
            val payment = if (selectedWechat) {
                PaymentClient.PaymentMethod.WxPay
            } else {
                PaymentClient.PaymentMethod.Alipay
            }
            mRealVM.rechargeShrimp(payment, goodsCode)
        }
        binding.tvRechargeAgreement.setOnSingleClickListener {
            SimpleBrowserActivity.launch(this, "https://www.baidu.com")
        }

        updatePaymentSelected(selectedWechat)
        mRealVM.getRechargePackages()
    }

    private fun initObservers() {
        mRealVM.userPoints.observe(this) { points ->
            binding.tvAvailableSticks.text = points?.takeIf { it.isNotBlank() } ?: "0"
        }
        mRealVM.rechargePackages.observe(this) { packages ->
            submitRechargePackages(packages.orEmpty())
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

    private fun updatePaymentSelected(wechat: Boolean) {
        selectedWechat = wechat
        val normalIcon = R.drawable.ic_choose_normal_dark
        binding.ivWechatSelected.setImageResource(
            if (wechat) R.drawable.icon_choose_checked else normalIcon
        )
        binding.ivAlipaySelected.setImageResource(
            if (wechat) normalIcon else R.drawable.icon_choose_checked
        )
    }
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
