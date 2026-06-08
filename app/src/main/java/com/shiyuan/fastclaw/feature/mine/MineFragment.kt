package com.shiyuan.fastclaw.feature.mine

import android.view.View
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.onUserStatusChanged
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.databinding.FragmentMineBinding
import com.shiyuan.fastclaw.feature.information.dialog.WorkExperienceDialog
import com.shiyuan.fastclaw.feature.main.MainViewModel
import com.shiyuan.fastclaw.feature.settings.SettingsActivity
import com.shiyuan.fastclaw.router.AppNavigator
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.isNull
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.feature.mine.viewModel.MineViewModel

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的
 * @Data: 2025/4/18  14:08
 * @Version: 1.0
 * *****************************************
 */
class MineFragment : BaseViewModelFragment<FragmentMineBinding, MineViewModel>() {

    private var userId: String? = null
    private val parentVm by lazy {
        getViewModelOfActivity<MainViewModel>()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        mRealVM.userPoints.observe(this){
            binding.tvPoints.text = it ?:"0"
        }
        mRealVM.isLogin.observe(this,{
            if (it) {
                User.get().userId?.let { uid ->
                    val firstLogin = Config.get().isFirstLogin(uid)
                    binding.llMinePointsTip.visibility = if (firstLogin) View.VISIBLE else View.GONE
                }
            } else {
                binding.llMinePointsTip.visibility = View.GONE
            }
            if(!it){
                binding.tvPoints.text = "0"
            }
        })
        mRealVM.underStandingScore.observe(this){
            it?.understandingScore?.let {
                if(it== 0 || it.isNull()){
                    binding.waveView.progress =10
                    binding.waveView.text ="${0}%"
                }else{
                    binding.waveView.progress =it
                    binding.waveView.text ="${it}%"
                }
            }
        }
        binding.sivAvatar.setOnSingleClickListener {
 /*           WorkExperienceDialog.show(
                childFragmentManager,
                arrayListOf(
                    ResumeData(
                        resumeFileUrl = "https://example.com/resume/tencent-ui.pdf",
                        isJd = false,
                        resumeFileName = "UI设计师-张三.pdf",
                        resumeFileType = "pdf",
                        submitTime = "2021.09 - 2024.05",
                        fileName = "高级UI设计师",
                        resumeName = "腾讯科技（深圳）有限公司",
                        blockingMessage = "内容：负责设计系统搭建、核心业务改版及跨端体验统一，推动多个版本迭代上线。"
                    ),
                    ResumeData(
                        resumeFileUrl = "https://example.com/resume/bytedance-product.pdf",
                        isJd = false,
                        resumeFileName = "产品经理-李四.pdf",
                        resumeFileType = "pdf",
                        submitTime = "2019.03 - 2021.08",
                        fileName = "高级产品经理",
                        resumeName = "北京字节跳动科技有限公司",
                        blockingMessage = "内容：主导增长产品策略、需求拆解与数据复盘，协同研发与设计推进项目落地。"
                    ),
                    ResumeData(
                        resumeFileUrl = "https://example.com/resume/alibaba-operation.pdf",
                        isJd = false,
                        resumeFileName = "运营主管-王五.pdf",
                        resumeFileType = "pdf",
                        submitTime = "2017.06 - 2019.02",
                        fileName = "运营主管",
                        resumeName = "阿里巴巴（中国）网络技术有限公司",
                        blockingMessage = "内容：负责活动运营、用户增长和渠道合作，搭建标准化运营流程并提升转化效率。"
                    )
                ),
                onAddClick = {
                    "点击了添加工作经历".showToast()
                },
                onItemClick = {
                    "点击了：${it.resumeName ?: it.resumeFileName.orEmpty()}".showToast()
                }
            )*/
        }

    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) return
        //更新必要信息
        mRealVM.updateInformation()
    }

    override fun onResume() {
        super.onResume()
        //更新必要信息
        mRealVM.updateInformation()
    }

    private fun dataUpdate() {
        observeEvents {
            /**
             * 登录状态变化
             */
            onUserStatusChanged {
                mRealVM.refreshUserInfo()
                if (userId == User.get().id) return@onUserStatusChanged
                userId = User.get().id
            }

            /**
             * 用户信息变化
             */
            on<Events.UserDataEvent> {
                mRealVM.refreshUserInfo()
            }
        }
    }


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.tv_name -> {
                AppNavigator.checkLoginIntercepted(false)
            }
            R.id.layout_mine_point -> {
                if(!AppNavigator.checkLoginIntercepted(false)){
                    MyShrimpChipsActivity.start(requireActivity())
                }
            }
            R.id.ll_setting -> {
                SettingsActivity.start(requireActivity())
            }
            R.id.tv_mine_tip_btn -> {
                User.get().userId?.let {
                    Config.get().recordFirstLogin(it)
                    binding.llMinePointsTip.visibility = View.GONE
                }
            }
            R.id.layout_mine_bj -> {
                if(!AppNavigator.checkLoginIntercepted(false)){
                   MyResumeActivity.start(requireActivity())
                }
            }

            R.id.layout_mine_lj  -> {
                if(!AppNavigator.checkLoginIntercepted(false)){
                    ShrimpUnderstandsActivity.start(requireActivity(),mRealVM.underStandingScore.value?.understandingScore ?: 0)
                }
            }
   /*         R.id.siv_avatar,
            R.id.tv_login,
            R.id.tv_name -> {
                //个人资料
                AppNavigator.checkLoginIntercepted(false)
            }

            R.id.ll_qa -> {
                SimpleBrowserActivity.launch(activity, Config.get().appConfig.appHelpCenterUrl)
            }

            R.id.ll_settings -> {
                //设置
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                SettingsActivity.start(requireActivity())
            }

            R.id.tv_id, R.id.iv_copy -> {
                //复制
                User.get().userUid?.let {
                    ClipboardUtils.copyText(it)
                    getString(R.string.copy_success).showToast()
                }
            }

            R.id.suggestions_feedback -> {
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                //建议与反馈
                FeedbackActivity.start(topActivity)
            }

            R.id.btnMyBalance -> {
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
            }*/
        }
    }
}