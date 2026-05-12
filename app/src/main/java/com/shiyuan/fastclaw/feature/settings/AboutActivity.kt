package com.shiyuan.fastclaw.feature.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.shiyuan.fastclaw.feature.settings.about.AboutUsActivity
import com.shiyuan.fastclaw.feature.settings.feedback.FeedbackActivity
import com.shiyuan.fastclaw.feature.settings.logoff.LogoffActivity
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.kit.SignUtils
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.actionsheet.ActionSheetHelper
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.Session
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.databinding.ActivityAboutBinding
import com.shiyuan.fastclaw.databinding.ActivityAboutUsBinding

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:设置
 * @Data: 2025/4/18  16:03
 * @Version: 1.0
 * *****************************************
 */
class AboutActivity : BaseViewModelActivity< ActivityAboutBinding, SettingsViewModel>() {
    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, AboutActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
     binding.recordFilingInformation.setOnSingleClickListener {
         SimpleBrowserActivity.launch(
             this,
             Config.get().appConfig.beianNoJumpUrl,
             getString(R.string.record_filing_information)
         )

     }

        binding.businessLicense.setOnSingleClickListener {
            Log.d("TAG", "🏁 ======= 启动性能监控开始 =======")
            SimpleBrowserActivity.launch(
                this,
                Config.get().appConfig.beianNoJumpUrl,
                getString(R.string.record_filing_information)
            )

        }



    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

}