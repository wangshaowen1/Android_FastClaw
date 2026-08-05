package com.shiyuan.fastclaw.feature.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.util.Pair
import androidx.lifecycle.lifecycleScope
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_CHECK_UPDATE
import com.shiyuan.fastclaw.core.manager.AppEvent.KLX_CLICK_CONTACT_SERVICE
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.manager.UpgradeManager
import com.shiyuan.fastclaw.databinding.ActivityAboutBinding
import com.shiyuan.fastclaw.feature.PhotoPagerActivity
import com.shiyuan.fastclaw.feature.upgrade.UpgradeDialogFragment
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import kotlinx.coroutines.delay


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
    private val upgradeManager by lazy { UpgradeManager(this) }

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

        binding.checkForUpdates.setOnSingleClickListener {
            EventManager.onAppTrack(KLX_CLICK_CHECK_UPDATE)
           checkUpgrade()
        }
    }

    private fun checkUpgrade() {
        lifecycleScope.launchNoCrash {
            upgradeManager.upgrade(true)
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
