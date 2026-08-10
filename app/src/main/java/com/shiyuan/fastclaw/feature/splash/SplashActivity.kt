package com.shiyuan.fastclaw.feature.splash

import android.os.Bundle
import android.util.Log
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.manager.AdsManager
import com.shiyuan.fastclaw.core.manager.ULinkManager
import com.shiyuan.fastclaw.databinding.ActivitySplashBinding
import com.shiyuan.fastclaw.feature.login.PrivacyPolicyDialog
import com.shiyuan.fastclaw.feature.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @Introduce: 招聘启动页
 */
class SplashActivity : BaseViewModelActivity<ActivitySplashBinding, SplashViewModel>() {

    private var pendingDialog: PrivacyPolicyDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTranslucentStatus()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, _ ->
            view.setPadding(0, 0, 0, 0)
            WindowInsetsCompat.CONSUMED
        }
        initView()
        initObservers()
        checkLaunch()
    }

    private fun initView() {
        val uri = intent?.data ?: return
        Log.d("ULinkManager",uri.toString())
        if (App.instance.hasAgree()) {
            ULinkManager.handleUri(this, uri)
        } else {
            ULinkManager.cacheUri(uri)
        }
    }

    private fun initObservers() {
        mRealVM.launchDestination.observe(this) { destination ->
            // 不管登没登录，都跳转到首页
            val deepLink = (destination as? SplashViewModel.LaunchDestination.Home)?.deepLink
            MainActivity.start(this, deepLink.orEmpty())
            finish()
        }
    }

    private fun checkLaunch() {
        if (!mRealVM.hasAgreed) {
            pendingDialog = PrivacyPolicyDialog.show(
                supportFragmentManager,
                getString(R.string.app_agree)
            )
            pendingDialog?.isCancelable = false
            pendingDialog?.callback = { agreed ->
                if (agreed) {
                   onPolicyAgreed()
                } else {
                    finish()
                }
                pendingDialog = null
            }
            return
        }
        lifecycleScope.launch {
            delay(2000)
            mRealVM.checkPolicyAndLaunch()
        }
    }

    fun onPolicyAgreed() {
        if (Config.get().isFirstOpenApp) {
            Config.get().markAppOpenedWithNotify()
            AdsManager.initAdsReporter(this,BuildConfig.DEBUG)
           App.instance.initSdkAfterAgreeLicense()
        }
        mRealVM.onPolicyAgreed()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .transparentStatusBar()
            .transparentNavigationBar()
            .init()
    }
}
