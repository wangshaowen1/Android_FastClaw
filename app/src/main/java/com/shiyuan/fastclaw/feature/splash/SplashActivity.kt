package com.shiyuan.fastclaw.feature.splash

import android.os.Bundle
import android.text.TextUtils
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.gyf.immersionbar.ImmersionBar
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.databinding.ActivitySplashBinding
import com.shiyuan.fastclaw.feature.login.phone.PhoneLoginActivity
import com.shiyuan.fastclaw.feature.main.MainActivity
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.utils.Logger
import com.shiyuan.fastclaw.router.AppRouter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @Introduce: 招聘启动页
 */
class SplashActivity : BaseViewModelActivity<ActivitySplashBinding, SplashViewModel>(),
    PolicyDialog.PolicyCallback {

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
        // deep link 处理：若 App 已启动且被外部唤起，直接跳转
        if (!ActivityUtils.getActivityList().isEmpty()) {
            intent.data?.takeIf { !TextUtils.isEmpty(it.host) }?.toString()?.also {
                AppRouter.jump(it)
                finish()
                return
            }
        }
    }

    private fun initObservers() {
        mRealVM.launchDestination.observe(this) { destination ->
            when (destination) {
                is SplashViewModel.LaunchDestination.Login -> PhoneLoginActivity.start(false, false)
                is SplashViewModel.LaunchDestination.Home -> MainActivity.start(
                    this, destination.deepLink.orEmpty()
                )
                null -> { /* ignore */ }
            }
            finish()
        }
    }

    private fun checkLaunch() {
        if (!mRealVM.hasAgreed) {
            PolicyDialog().show(supportFragmentManager)
            return
        }
        lifecycleScope.launch {
            delay(2000)
            mRealVM.checkPolicyAndLaunch()
        }
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(false)
            .transparentStatusBar()
            .transparentNavigationBar()
            .init()
    }

    override fun onAgreePolicy(dialog: PolicyDialog) {
        dialog.dismissAllowingStateLoss()
        mRealVM.onPolicyAgreed()
    }

    override fun onRejectPolicy(dialog: PolicyDialog) {
        finish()
    }
}
