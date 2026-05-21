package com.shiyuan.fastclaw.feature.mine

import android.view.View
import com.blankj.utilcode.util.ClipboardUtils
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.topActivity
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.onUserStatusChanged
import com.shiyuan.fastclaw.databinding.FragmentMineBinding
import com.shiyuan.fastclaw.feature.main.MainViewModel
import com.shiyuan.fastclaw.feature.settings.SettingsActivity
import com.shiyuan.fastclaw.feature.settings.feedback.FeedbackActivity
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.router.AppNavigator
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getViewModelOfActivity
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
        mRealVM.isLogin.observe(this,{
            if (it) {
                User.get().userId?.let { uid ->
                    val firstLogin = Config.get().isFirstLogin(uid)
                    binding.tvMinePointsTip.visibility = if (firstLogin) View.VISIBLE else View.GONE
                    binding.tvMineTipBtn.visibility = if (firstLogin) View.VISIBLE else View.GONE
                }
                binding.layoutMineBj.visibility = View.VISIBLE
            } else {
                binding.tvMinePointsTip.visibility = View.GONE
                binding.tvMineTipBtn.visibility = View.GONE
                binding.layoutMineLj.visibility = View.VISIBLE
                binding.layoutMineBj.visibility = View.GONE
            }
            if(!it){
                binding.tvPoints.text = "-"
            }
        })
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
            R.id.iv_setting -> {
                SettingsActivity.start(requireActivity())
            }
            R.id.tv_mine_tip_btn -> {
                User.get().userId?.let {
                    Config.get().recordFirstLogin(it)
                    binding.tvMinePointsTip.visibility = View.GONE
                    binding.tvMineTipBtn.visibility = View.GONE
                }
            }
            R.id.layout_mine_bj -> {
                if(!AppNavigator.checkLoginIntercepted(false)){
                    MyResumeActivity.start(requireActivity())
                }
            }

            R.id.layout_mine_lj  -> {
                if(!AppNavigator.checkLoginIntercepted(false)){
                    ShrimpUnderstandsActivity.start(requireActivity())
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