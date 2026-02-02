package com.kgmedia.fastfilm.feature.payment.vip

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.GrowingIO
import com.kgmedia.fastfilm.core.network.apis.entity.VipConfigInfo
import com.kgmedia.fastfilm.databinding.ActivityMembershipBenefitsBinding
import com.kgmedia.fastfilm.feature.payment.PaymentMethodDialog
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.kuaige.payment.PaymentClient
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:会员权益
 * @Data: 2025/4/21  9:31
 * @Version: 1.0
 * *****************************************
 */
class MembershipBenefitsActivity :
    BaseViewModelActivity<ActivityMembershipBenefitsBinding, MembershipBenefitsViewModel>() {
    enum class Scene(val value: Int = 0) {
        Mine,

        /**
         * 配音
         */
        Dubbing,
        CustomCreationStyle,
        InspirationCreationStyle,
        AllCreationStyle,
        MakeSame,
        InputMaxLimit,
        RoleSetting,
        CloneVoice,

        /**
         * 下载短剧
         */
        Download,

        //创作收益
        CreativeIncome,

        /**
         * 上架小说
         */
        ReleaseSeries,
    }

    companion object {
        private const val SCENE = "SCENE"

        /**
         * 会员权益
         * @param activity Activity?
         * @param scene Scene? 类型
         * @param withToast Boolean? 是否显示Toast
         */
        fun start(
            activity: Activity? = null,
            scene: Scene? = null,
            withToast: Boolean? = true,
        ) {
            val context = activity ?: topActivity ?: return
            if (withToast == true) {
                when (scene) {
                    Scene.InputMaxLimit -> "开通会员后即可提高字数限制".showToast()
                    Scene.MakeSame -> "开通会员可使用专属爆款剧本".showToast()
                    Scene.RoleSetting -> "开通VIP可解锁主角形象".showToast()
                    Scene.Dubbing -> "开通会员后即可使用专属音色".showToast()
                    Scene.InspirationCreationStyle, Scene.CustomCreationStyle, Scene.AllCreationStyle -> "开通会员后可使用专属短剧风格".showToast()
                    Scene.CloneVoice -> "开通VIP可克隆音色".showToast()
                    Scene.Download -> "开通会员即可下载无水印视频".showToast()
                    Scene.ReleaseSeries -> "开通会员即可上架小说哦~".showToast()
                    else -> "请先开通VIP".showToast()
                }
            }
            val intent = Intent(context, MembershipBenefitsActivity::class.java)
            if (scene != null) {
                intent.putExtra(SCENE, scene.ordinal)
            }
            context.startActivity(intent)
        }
    }

    private val memberAdapter =
        QuickAdapter<ItemMembershipBenefitsVm>(R.layout.item_member, arrayListOf())
    private val benefitsAdapter = QuickAdapter<ItemMembershipBenefitsPrivilegeVm>(
        R.layout.item_member_benefits,
        arrayListOf()
    )

    //跳转进来的方式
    private var scene: Scene? = null

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        scene = Scene.entries.getOrNull(intent.getIntExtra(SCENE, 0)) ?: Scene.Mine
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.rvMember.adapter = memberAdapter
        binding.rvBenefits.adapter = benefitsAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun dataUpdate() {
        mRealVM.vipConfigInfo.observe(this) {
            //选中的项
            var lastSelected: ItemMembershipBenefitsVm? = null
            val membershipBenefitsList = it?.items?.map { mc ->
                ItemMembershipBenefitsVm(mc).also { membershipBenefitsVm ->
                    if (mc.isSelect() && lastSelected == null) {
                        //还没有选中的
                        membershipBenefitsVm.isSelected.set(true)
                        lastSelected = membershipBenefitsVm
                    }
                    membershipBenefitsVm.onItemClick.value = onItemClick@{
                        if (membershipBenefitsVm.isSelected.get()) {
                            return@onItemClick
                        }
                        memberAdapter.items.find { vm ->
                            vm.isSelected.get()
                        }?.also { selectedVm ->
                            //取消之前选中的项目
                            selectedVm.isSelected.set(false)
                        }
                        //设置选中的参数
                        setSelectedParameters(membershipBenefitsVm.data.get())
                        membershipBenefitsVm.isSelected.set(true)
                    }
                }
            }

            if (lastSelected == null) {
                //没有选择
                membershipBenefitsList?.firstOrNull()?.isSelected?.set(true)
            }
            memberAdapter.submitList(membershipBenefitsList)
            //设置选中的参数
            setSelectedParameters(lastSelected?.data?.get())

            val valueList =
                it?.vipPrivilegesArray?.map { vr -> ItemMembershipBenefitsPrivilegeVm(vr) }
            benefitsAdapter.submitList(valueList)
        }

        mRealVM.menuAction.observe(this) {
            if (!it) return@observe
            mRealVM.menuAction.value = false
            MembershipInstructionsDialog.show(supportFragmentManager)
        }
    }

    /**
     *设置选中的参数
     */
    private fun setSelectedParameters(membershipConfigurationInfo: VipConfigInfo.MembershipConfigurationInfo?) {
        if (membershipConfigurationInfo == null) return
        //设置介绍文本
        mRealVM.desc.set(membershipConfigurationInfo.vipRichTextDesc)
        mRealVM.selectedConfig = membershipConfigurationInfo
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnBuy -> {
                buy()
            }

            R.id.tvPolicyRight -> {
                viewPolicy()
            }
        }
    }

    private fun buy() {

        memberAdapter.items.find { vm -> vm.isSelected.get() }?.let { vm ->
            EventManager.onAppTrack(
                AppEvent.JG_CLICK_MY_PAGE_OPEN_NOW_BUTTON,
                mapOf(GrowingIO.JG_VIP_TYPE to (vm.data.get()?.vipType?:0).toString())
            )
            mRealVM.paymentTerms = vm.data.get()
            //这个是微信加支付宝支付的模式
            PaymentMethodDialog.verifyThePaymentConditions(
                supportFragmentManager, onConfirmClick = { isWechat ->
                    mRealVM.startPayment(
                        paymentMethod = if (isWechat) PaymentClient.PaymentMethod.WxPay else PaymentClient.PaymentMethod.Alipay,
                        vipConfigId = mRealVM.paymentTerms?.vipConfigId ?: "",
                    )
                }, paymentType = PaymentMethodDialog.CREATOR_RIGHTS,
                amount = vm.membershipAmount()
            )
        }
    }

    private fun viewPolicy() {
        SimpleBrowserActivity.launch(
            this@MembershipBenefitsActivity,
            Config.get().userVipProtocolUrl,
            "付费会员协议"
        )
    }
}