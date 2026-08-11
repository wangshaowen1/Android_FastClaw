package com.shiyuan.fastclaw.feature.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.databinding.ActivityRecordFilingInfomationBinding
import com.shiyuan.fastclaw.feature.settings.vm.SettingsViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity


class RecordFilingInformationActivity :
    BaseViewModelActivity<ActivityRecordFilingInfomationBinding, SettingsViewModel>() {
    companion object {
        private const val RECORD_FILING_URL = "https://beian.miit.gov.cn"

        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, RecordFilingInformationActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.tvRecordName.setOnSingleClickListener {
            SimpleBrowserActivity.launch(
                this,
                RECORD_FILING_URL,
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
