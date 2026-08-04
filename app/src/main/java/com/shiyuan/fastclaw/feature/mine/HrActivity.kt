package com.shiyuan.fastclaw.feature.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.blankj.utilcode.util.ClipboardUtils
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.ext.openWithBrowser
import com.shiyuan.fastclaw.databinding.ActivityHrBinding
import com.shiyuan.fastclaw.feature.mine.viewModel.HrViewModel

class HrActivity : BaseViewModelActivity<ActivityHrBinding, HrViewModel>() {

    companion object {
        private const val HR_URL = "http://kuailongxia.com"

        fun start(activity: Activity?) {
            activity ?: return
            activity.startActivity(Intent(activity, HrActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.ivBack.setOnSingleClickListener { finish() }
        binding.viewCopyLink.setOnSingleClickListener {
            ClipboardUtils.copyText(HR_URL)
            getString(R.string.copy_success).showToast()
            //HR_URL.openWithBrowser()
        }
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_hr_navigation_bar)
            .transparentStatusBar()
            .init()
    }
}
