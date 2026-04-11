package com.kgmedia.fastfilm.feature.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.bytedance.ads.convert.BDConvert.init
import com.bytedance.ads.convert.callback.BDConvertLifecycleCallback
import com.bytedance.ads.convert.config.BDConvertConfig
import com.gyf.immersionbar.ImmersionBar
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.BuildConfig
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.databinding.ActivitySplashBinding
import com.kgmedia.fastfilm.feature.main.MainActivity
import com.kgmedia.fastfilm.feature.splash.newer.GuideStep1AdActivity
import com.kgmedia.fastfilm.router.AppRouter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject


/**
 * 启动页
 */
class SplashActivity : BaseViewModelActivity<ActivitySplashBinding, SplashViewModel>(),
    PolicyDialog.PolicyCallback {
    companion object {
        private const val TAG = "SplashActivity"
    }

    var lottieHasEnd = false
    var isFinish = false
    @Volatile
    var hasAgree = false

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val splashScreen = installSplashScreen()
            splashScreen.setOnExitAnimationListener { it.remove()}
            super.onCreate(savedInstanceState)
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
                // 强行把根布局的内边距清零
                view.setPadding(0, 0, 0, 0)
                // 返回 CONSUMED，告诉系统：“安全边距我处理完了，不要再去压缩我的 View 的高度了！”
                WindowInsetsCompat.CONSUMED
            }
            hasAgree = App.instance.hasAgree()
            binding.splashLottie.addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    lottieHasEnd = true
                    Logger.d("是否同意协议了：onAnimationEnd===isFinish = ${isFinish}")
                    if (isFinish) return
                    Logger.d("是否同意协议了：onAnimationEnd===hasAgree =${hasAgree}")
                    if (hasAgree) {
                        jumpToTheHomepage()
                    }
                    super.onAnimationEnd(animation)
                }
            })
            if (!isTaskRoot) {
                intent.data?.takeIf { !TextUtils.isEmpty(it.host) }?.toString()?.also {
                    if (ActivityUtils.getActivityList().find { activity -> activity.javaClass == MainActivity::class.java } != null) {
                        AppRouter.jump(it)
                        finish()
                    } else {
                        jumpToTheHomepage()
                    }
                } ?: run { finish() }
                return
            }
            next()
        } catch (e: Throwable) {
            e.printStackTrace()
            next()
        }
    }

    private fun next() {
        if (!App.instance.hasAgree()) {
            PolicyDialog().show(supportFragmentManager)
            return
        }
        lifecycleScope.launch {
            initAdsReporter()
            delay(2000)
            jumpToTheHomepage()
        }
    }

    private fun jumpToTheHomepage() {
        val it = intent.data?.takeIf { !TextUtils.isEmpty(it.host) } ?: ""
        if (Config.get().isFirstOpenApp) {
            Config.get().markAppOpenedWithNotify()
            App.instance.initSdkAfterAgreeLicense()
            hasAgree = true
            Logger.d("是否同意协议了：jumpToTheHomepage==1=hasAgree =${hasAgree}")
            if (!lottieHasEnd) return
            Logger.d("是否同意协议了：jumpToTheHomepage==1=lottieHasEnd =${lottieHasEnd}")
            isFinish = true
            GuideStep1AdActivity.start(this)
        } else {
            Logger.d("是否同意协议了：jumpToTheHomepage==2=hasAgree =${hasAgree}")
            if (!lottieHasEnd) return
            Logger.d("是否同意协议了：jumpToTheHomepage==2=lottieHasEnd =${lottieHasEnd}")
            isFinish = true
            MainActivity.start(this@SplashActivity, it.toString())
        }
        finish()
    }


    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(false)
            .transparentStatusBar()
            .transparentNavigationBar()
            .init()
    }

    /**
     * 归因处理
     */
    private fun initAdsReporter() {
        val context: Context = this
        val activity: Activity = this
        Thread {//文档说不建议用协程。。。
            try {
                val config = BDConvertConfig()
                // 在调用init时是否自动上报事件，默认为true，
                config.autoSendLaunchEvent = true
                // 是否开启debug日志，默认为false，在排查问题时使用,参考：https://bytedance.larkoffice.com/docx/D99KdC7VXoMmarxIza6cAFKDnhe
                config.enableLog = BuildConfig.DEBUG
                // 是否启用心跳事件，默认为true
                config.playSessionEnable = true
                // SDK是否采集OAID，默认为true
                config.enableOAID = true
                // 配置自定义OAID获取逻辑，默认不配置；若配置，则SDK自身不会有采集OAID的行为，会调用这个Callback获取
//                config.customOaidCallback = object : CustomOaidCallback {
//                    override fun get(): String {
//                        //"自定义OAID获取"
//                        return ""
//                    }
//                }
//                // 配置自定义Android_id获取逻辑，默认不配置；若配置，则SDK自身不会有采集android_id的行为，会调用这个Callback获取
//                // 请正确回传Android_id值，错误传值会导致归因失败
//                config.customAndroidIDCallback = object : CustomAndroidIDCallback {
//                    override fun get(): String {
//                        //"自定义Android_ID获取"
//                        return ""
//                    }
//                }
                config.lifecycleCallback = (object : BDConvertLifecycleCallback {
                    override fun onOtherError(reason: Int, throwable: Throwable?) {
                        // 如果有非打断的错误，会触发该回调
                        Logger.e(TAG, "onOtherError: $reason ${throwable?.message}")
                        val params = JSONObject()
                        try {
                            params.put("reason", reason)
                            params.put("error_msg", throwable?.message ?: "")
                            //sendHostLog("bd_convert_other_error", params)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onEventSendFailure(eventName: String, reason: Int, requestId: String, throwable: Throwable?) {
                        // 事件发送发生错误时，会触发该回调
                        Logger.e(TAG, "onEventSendFailure: $eventName-$reason $requestId  ${throwable?.message}")
                        val params = JSONObject()
                        try {
                            params.put("event_name", eventName)
                            params.put("reason", reason)
                            params.put("request_id", requestId)
                            params.put("error_msg", throwable?.message ?: "")
                            //sendHostLog("bd_convert_event_send_failure", params)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onInitFailure(reason: Int, throwable: Throwable?) {
                        // 初始化发生错误时，会触发该回调
                        Logger.e(TAG, "onInitFailure: $reason  ${throwable?.message}")
                        val params = JSONObject()
                        try {
                            params.put("reason", reason)
                            params.put("error_msg", throwable?.message ?: "")
                            //sendHostLog("bd_convert_init_failure", params)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onInitSuccess() {
                        // 初始化成功回调
                        Logger.d(TAG, "onInitSuccess")
                        //sendHostLog("bd_convert_init_success", JSONObject())
                    }

                    override fun onEventSendSuccess(eventName: String, requestId: String) {
                        // 事件发送成功回调
                        Logger.d(TAG, "onEventSendSuccess: $eventName-$requestId")
                        val params = JSONObject()
                        try {
                            params.put("event_name", eventName)
                            params.put("request_id", requestId)
                            //sendHostLog("bd_convert_event_send_success", params)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                })
                init(context, config, activity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onAgreePolicy(dialog: PolicyDialog) {
        dialog.dismissAllowingStateLoss()
        initAdsReporter()
        jumpToTheHomepage()
    }


    override fun onRejectPolicy(dialog: PolicyDialog) {
        finish()
    }
}
