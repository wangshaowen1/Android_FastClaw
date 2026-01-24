package com.kgmedia.fastfilm.feature.payment.points

import android.app.Activity
import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constant
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.databinding.ActivityPointsRechargeBinding
import com.kgmedia.fastfilm.feature.payment.ItemPaymentMethodVm
import com.kgmedia.fastfilm.feature.payment.PaymentMethod
import com.kgmedia.fastfilm.feature.payment.points.details.PointsDetailActivity
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kgmedia.fastfilm.widget.decoration.SpaceDecoration
import com.kuaige.payment.PaymentClient
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import java.util.concurrent.atomic.AtomicBoolean

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的余额
 * @Data: 2025/9/10/周三  17:47
 * @Version: 1.0
 * *****************************************
 */
class PointsRechargeActivity :
    BaseViewModelActivity<ActivityPointsRechargeBinding, PointsRechargeViewModel>() {

    companion object {

        fun start(activity: Activity?, withToast: Boolean? = true) {
            val activity = activity ?: topActivity
            if (withToast == true) {
                "当前余额不足，请充值".showToast()
            }
            val intent = Intent(activity, PointsRechargeActivity::class.java)
            activity?.startActivity(intent)
        }
    }

    private val topUpAdapter by lazy {
        QuickAdapter<ItemPointsRechargeVm>(R.layout.item_points_recharge, emptyList())
    }

    //当前选择的是微信(true)还是支付宝(false)
    private val isWechat = AtomicBoolean(false)
    private val wayAdapter by lazy {
        QuickAdapter<ItemPaymentMethodVm>(R.layout.item_payment_method_points_recharge, emptyList())
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()

        EventManager.onAppTrack(
            AppEvent.JG_VIEW_MY_BALANCE_PAGE
        )
        initView()
        //数据更新
        dataUpdate()
        EventManager.onAppTrack(AppEvent.JG_VIEW_BUY_INTEGRAL_PAGE)
    }

    private fun initView() {
        binding.srl.apply {
            setOnRefreshListener {
                //权益配置
                mRealVM.refreshType.set(true)
                mRealVM.pointsProgram()
                mRealVM.getUserHomeInfo()
            }
            //是否启用下拉刷新
            setEnableRefresh(true)
            //是否启用上拉加载功能
            setEnableLoadMore(false)
        }

        binding.rvPaymentMethod.apply {
            addItemDecoration(SpaceDecoration(8.dp))
            adapter = wayAdapter
        }
        binding.recyclerView.adapter = topUpAdapter

        //支付协议
        paymentProtocol()
    }

    private fun dataUpdate() {
        Config.get().appConfig.paymentStatus?.apply {
            val data = arrayListOf<ItemPaymentMethodVm>()
            val zfb = ItemPaymentMethodVm(
                PaymentMethod(
                    name = R.string.alipay_payment.getResString(),
                    icon = R.drawable.ic_pm_alipay,
                    type = PaymentClient.PaymentMethod.Alipay
                )
            )
            val wx = ItemPaymentMethodVm(
                PaymentMethod(
                    name = R.string.wechat_pay.getResString(),
                    icon = R.drawable.ic_pm_wechat,
                    type = PaymentClient.PaymentMethod.WxPay
                )
            )
            zfb.onItemClick.value = {
                isWechat.set(false)
                zfb.select.set(true)
                wx.select.set(false)
            }
            wx.onItemClick.value = {
                isWechat.set(true)
                zfb.select.set(false)
                wx.select.set(true)
            }
            //是否支持支付宝
            val isShowAlipay = alipay?.status == 1
            //是否支持微信
            val isShowWechat = if (Constant.SDKConfiguration.IS_SHOW_WE_CHAT) wxpay?.status == 1 else false
            when {
                !isShowAlipay && !isShowWechat -> {
                    //都不支持支付方式，这种情况需要特殊处理
                    //当前实现是不添加任何支付方式到列表中
                }

                !isShowWechat -> {
                    //不支持微信那就是只支持支付宝
                    zfb.select.set(true)
                    data.add(zfb)
                }

                !isShowAlipay -> {
                    //不支持支付宝那就是只支持微信
                    wx.select.set(true)
                    isWechat.set(true)
                    data.add(wx)
                }

                else -> {
                    // 两者都支持，根据排序值决定顺序
                    val alipayShort = alipay.short ?: 0
                    val wxPayShort = wxpay?.short ?: 0

                    if (alipayShort >= wxPayShort) {
                        zfb.select.set(true)
                        isWechat.set(false)
                        data.add(zfb)
                        data.add(wx)
                    } else {
                        wx.select.set(true)
                        isWechat.set(true)
                        data.add(wx)
                        data.add(zfb)
                    }
                }
            }
            wayAdapter.submitList(data)
        }
        mRealVM.onMenuClick.observe(this) {
            if (!it) return@observe
            mRealVM.onMenuClick.value = false
            PointsDetailActivity.start(this)
        }

        mRealVM.pointsRechargeData.observe(this) {
            val pointsRechargeVm = it?.items?.map { pointsRechargeData ->
                ItemPointsRechargeVm(pointsRechargeData).apply {
                    onItemClick.value = onItemClick@{
                        if (select.get() == true) return@onItemClick
                        topUpAdapter.items.firstOrNull { pointsRechargeVm ->
                            pointsRechargeVm.select.get() == true
                        }?.select?.set(false)
                        select.set(true)
                    }
                }
            }?.apply {
                firstOrNull()?.select?.set(true)
            }
            topUpAdapter.submitList(pointsRechargeVm)
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.tv_payment -> {
                //支付
                val pointsRechargeData = topUpAdapter.items.firstOrNull { pointsRechargeVm ->
                    pointsRechargeVm.select.get() == true
                }?.data?.get() ?: return
                mRealVM.startPayment(
                    paymentMethod = if (isWechat.get()) PaymentClient.PaymentMethod.WxPay else PaymentClient.PaymentMethod.Alipay,
                    pointsData = pointsRechargeData
                )
            }
        }
    }

    private fun paymentProtocol() {
        val spannableString = SpannableString("充值即代表同意《充值协议》")
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                SimpleBrowserActivity.launch(
                    this@PointsRechargeActivity,
                    Config.get().appConfig.clientPaymentProtocolUrl,
                    "充值协议"
                )
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.setColor(
                    ContextCompat.getColor(
                        this@PointsRechargeActivity,
                        R.color.color_121212
                    )
                )
            }
        }, 7, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvAgreement.movementMethod = LinkMovementMethod.getInstance()
        binding.tvAgreement.text = spannableString
    }
}