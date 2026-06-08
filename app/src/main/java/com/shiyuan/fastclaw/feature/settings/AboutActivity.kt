package com.shiyuan.fastclaw.feature.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.util.Pair
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.databinding.ActivityAboutBinding
import com.shiyuan.fastclaw.feature.PhotoPagerActivity
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity


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
         RecordFilingInformationActivity.start(this)
     }

        binding.businessLicense.setOnSingleClickListener {
            val uri = "android.resource://" + getPackageName() + "/" + R.mipmap.business_license
            val uris = ArrayList<String?>()
            uris.add(uri)
            PhotoPagerActivity. launch(this, uris) // 复用现有的 launch 方法
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