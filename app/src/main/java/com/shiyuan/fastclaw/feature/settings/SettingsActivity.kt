package com.shiyuan.fastclaw.feature.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.showConfirmPrompt
import com.shiyuan.fastclaw.common.utils.DebugUtils
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.ext.openTheCustomerServiceChat
import com.shiyuan.fastclaw.databinding.ActivitySettingsBinding
import com.shiyuan.fastclaw.feature.login.flash.PhoneLoginDialogActivity
import com.shiyuan.fastclaw.feature.mine.PhoneShowActivity
import com.shiyuan.fastclaw.feature.settings.logoff.LogoffActivity
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.kit.SignUtils
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.actionsheet.ActionSheetHelper
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.core.ext.customerServiceTransfer
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_CONTACT_SERVICE
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_INVITE_RULES
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_LOGOUT
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_VIEW_SETTINGS
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.feature.settings.dialog.LogoutConfirmDialog

/**
 * *****************************************
 * @Introduce:设置
 */
class SettingsActivity : BaseViewModelActivity<ActivitySettingsBinding, SettingsViewModel>() {
    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
/*        if (DebugUtils.isDebug()) {
            binding.etIp.setText(Config.get().baseUrlCustom)
            binding.etIp.setSelection(binding.etIp.length())
            binding.btnIPConfirm.setOnSingleClickListener {
                val url = mRealVM.debugIp.get() ?: BuildConfig.SERVER_URL
                Config.get().storeBaseUrlCustom(url)
                "设置好了，自己杀掉app重启一下！".showToast()
            }
            binding.btnIPSelect.setOnSingleClickListener {
                ActionSheetHelper.createDialog(DebugUtils.ipsName) { _, position ->
                    val ip = DebugUtils.ips[position]
                    Config.get().storeBaseUrlCustom(ip)
                    binding.etIp.setText(ip)
                    binding.etIp.setSelection(binding.etIp.length())
                    "设置好了，自己杀掉app重启一下！".showToast()
                }.show(supportFragmentManager)
            }
        }*/
        initView()

    }

    private fun initView() {
        if (User.get().isVisitor) {
            binding.rlAccount.visibility = View.GONE
            binding.rlLogOut.visibility = View.GONE
        } else {
            binding.rlAccount.visibility = View.VISIBLE
            binding.rlLogOut.visibility = View.VISIBLE
        }
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.rlAccount -> {
                EventManager.onAppTrack(KLX_VIEW_SETTINGS)
                AccountSettingsActivity.start(this)
            }
            R.id.rl1 -> {
                //用户协议
                SimpleBrowserActivity.launch(
                    this,
                    Config.get().serviceUrl,
                    getString(R.string.app_user_agreement)
                )
            }
            R.id.rl2 -> {
                //隐私政策
                SimpleBrowserActivity.launch(
                    this,
                    Config.get().privacyUrl,
                    getString(R.string.app_privacy_policy),
                )

            }
            R.id.rl3 -> {
                AboutActivity.start(this)
            }
            R.id.rlLogOut -> {
                if( User.get().isVisitor){
                    // 登录
                    AppNavigator.checkLoginIntercepted(true)
                }else{
                    showConfirmLogoutDialog()
                }
            }
            R.id.rl5 -> {
                EventManager.onAppTrack(KLX_CLICK_CONTACT_SERVICE)
                customerServiceTransfer()
            }
        }
    }

    private fun showConfirmLogoutDialog() {
        val confirmDialog = LogoutConfirmDialog.show(supportFragmentManager)
        confirmDialog.clickListener = {
            if (it) {
                //退出登录
                EventManager.onAppTrack(KLX_CLICK_LOGOUT)
                mRealVM.logoutApp()
            }
        }
    }
}
