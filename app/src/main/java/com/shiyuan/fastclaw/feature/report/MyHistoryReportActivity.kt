package com.shiyuan.fastclaw.feature.report

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
import com.shiyuan.fastclaw.feature.settings.about.AboutUsActivity
import com.shiyuan.fastclaw.feature.settings.feedback.FeedbackActivity
import com.shiyuan.fastclaw.feature.settings.logoff.LogoffActivity
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.shiyuan.fastclaw.kit.SignUtils
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.widget.actionsheet.ActionSheetHelper
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.databinding.ActivityMyHistoryReportBinding
import com.shiyuan.fastclaw.feature.settings.SettingsViewModel

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:历史报告
 * @Data: 2025/4/18  16:03
 * @Version: 1.0
 * *****************************************
 */
class MyHistoryReportActivity : BaseViewModelActivity<ActivityMyHistoryReportBinding, ReportViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, MyHistoryReportActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

}