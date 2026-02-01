package com.kgmedia.fastfilm

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import androidx.annotation.NonNull
import androidx.fragment.app.DialogFragment
import cn.android.security.APISecurity
import com.alibaba.android.alpha.Task
import com.blankj.utilcode.util.ProcessUtils
import com.blankj.utilcode.util.Utils
import com.fm.openinstall.Configuration
import com.fm.openinstall.OpenInstall
import com.github.gzuliyujiang.oaid.DeviceID
import com.kgmedia.fastfilm.common.Cache
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.Oaid
import com.kgmedia.fastfilm.common.SP
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.UmInitConfig
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.getInstall
import com.kgmedia.fastfilm.core.manager.AdStrategyManager
import com.kgmedia.fastfilm.core.manager.FlashLoginManager
import com.kgmedia.fastfilm.core.manager.OssManager
import com.kgmedia.fastfilm.core.manager.PlaceManager
import com.kgmedia.fastfilm.core.manager.PushManager
import com.kgmedia.fastfilm.feature.main.MainActivity
import com.kgmedia.fastfilm.feature.splash.SplashActivity
import com.kgmedia.fastfilm.router.AppRouter
import com.kgmedia.fastfilm.widget.CommonDialog
import com.kgmedia.fastfilm.widget.CustomizedRefreshHeader
import com.kgmedia.fastfilm.widget.LoadingDialog2
import com.kgmedia.fastfilm.widget.MyLoadViewFactory
import com.growingio.android.sdk.autotrack.AutotrackConfiguration
import com.growingio.android.sdk.autotrack.GrowingAutotracker
import com.kuaige.oss.OssType
import com.kuaige.payment.PaymentConfig
import com.nhsd.basic.BaseApp
import com.nhsd.basic.Session
import com.nhsd.basic.manager.LoadingConfig
import com.nhsd.basic.manager.LoadingDialogFactory
import com.nhsd.basic.utils.Logger
import com.nhsd.mvc.MVCHelper
import com.nhsd.permisson.DefaultRationaleFactory
import com.nhsd.permisson.PermissionX
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.tencent.bugly.crashreport.CrashReport
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.Random

class App : BaseApp() {

    private lateinit var mainThreadHandler: Handler

    // 使用 lateinit 延迟初始化，避免空安全检查
    lateinit var config: Config
        private set
    lateinit var user: User
        private set

    var cache: Cache? = null
        private set

    lateinit var placeManager: PlaceManager
        private set

    var defaultProvince: String? = null
    var defaultCity: String? = null

    private lateinit var umInitConfig: UmInitConfig

    var ran: String? = null
        private set

    private lateinit var random: Random

    var isApiSecurityInit: Boolean = false
        private set

    private lateinit var appStatusTracker: AppStatusTracker

    // 使用 object 表达式替代匿名内部类
    private val sdkConfigTask = object : Task("sdkConfigTask") {
        override fun run() {
            initSdkAfterAgreeLicense()
            fileManager.providerName = "fileprovider"

            if (ProcessUtils.isMainProcess()) {
                val releaseEnv = listOf("production", "preview")
                val releaseDomain = listOf("http://120.24.240.28:9010", "https://jk.jinguomanju.com")

                val apiSecurityStartTime = System.currentTimeMillis()
                // 注意：Kotlin 中访问静态/伴生对象通常直接用类名，或者 companion 里的常量
                // 假设 BuildConfig.FLAVOR_env 是生成的静态常量
                APISecurity.init(
                    this@App,
                    releaseEnv.contains(BuildConfig.FLAVOR_env) || releaseDomain.contains(Config.get().baseUrlCustom)
                )
                isApiSecurityInit = true
                Logger.i("starttime,api security use =" + (System.currentTimeMillis() - apiSecurityStartTime))
                config.initAppConfig()
            }

            PermissionX.setRationaleFactory(object : DefaultRationaleFactory() {
                var positiveCheck = false

                @NonNull
                override fun createDeniedForeverRationale(
                    @NonNull context: Context,
                    @NonNull permissions: Array<String>,
                    @NonNull positive: Runnable,
                    @NonNull negative: Runnable
                ): Dialog {
                    val namesString = permissionNameStr(permissions, context)
                    positiveCheck = false
                    val commonDialog = CommonDialog(
                        context,
                        context.getString(
                            com.lws.permissionx.R.string.permissionx_default_denied_forever_rationale_message,
                            namesString
                        )
                    )
                    commonDialog.setPositiveButtonText("去开启")
                    commonDialog.onPositiveButtonClickListener = { _ ->
                        positive.run()
                        positiveCheck = true
                    }
                    commonDialog.setOnDismissListener { _ ->
                        if (!positiveCheck) {
                            negative.run()
                        }
                    }
                    return commonDialog
                }
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

//                StrictMode.setVmPolicy(
//            StrictMode.VmPolicy.Builder()
//                .detectActivityLeaks()//Activity是否内存泄漏
//                .detectLeakedSqlLiteObjects()//数据库是否未关闭
//                .detectLeakedClosableObjects()//文件是否未关闭
//                .setClassInstanceLimit(MainActivity::class.java,1) //某个类在内存中实例上限
//                .detectLeakedRegistrationObjects()//对象是否被正确关闭
//                .penaltyLog()//打印日志，可以通过 StrictMode 来过滤
//                //.penaltyDeath()//直接Crash掉当前应用程序
//                .build()
//        )

        val start = System.currentTimeMillis()
        Logger.i("App_", "starttime:$start")
        // 使用属性访问 isDebug
        Logger.setDebug(isDebug)

        SP.init(this)
        config = Config()
        user = User()
        appStatusTracker = AppStatusTracker()
        registerActivityLifecycleCallbacks(appStatusTracker)

        mainThreadHandler = Handler(Looper.getMainLooper())

        val objectId = AdStrategyManager.getDefaultObjectId()
        config.storeObjectId(objectId)

        MVCHelper.setLoadViewFactory(MyLoadViewFactory())

        placeManager = PlaceManager()
        placeManager.asynInitRegion()

        random = Random()
        ran = "100000"

        umInitConfig = UmInitConfig(applicationContext)
        OpenInstall.setDebug(BuildConfig.DEBUG)
        OpenInstall.preInit(this)

        Logger.i("App_", "starttime use:" + (System.currentTimeMillis() - start))

        builder.add(sdkConfigTask)
        isApiSecurityInit = false
        startTasks(builder)

        // SAM 转换：直接使用 Lambda
        LoadingConfig.setFactory { LoadingDialog2() }
        OssManager.init(ossType = OssType.ALIYUN, ossFileNameProvider = {ossPathName, suffix ->
            val timestamp = System.currentTimeMillis()
            "FFApp/$ossPathName/${User.get().id}/0$timestamp.$suffix"
        })
        // 初始化加载刷新控件
        initSmartRefreshLayoutView()
//todo 微信支付加上后需要打开这里        PaymentConfig.init(BuildConfig.WX_APP_ID, BuildConfig.WX_APP_SECRET)
        initGrowingIO()
    }

    class AppStatusTracker : ActivityLifecycleCallbacks {
        /**
         * 当前是否有activity在运行
         */
        var activityCount = 0

        /**
         * 是否是在后台
         */
        var isBackground = false
            private set

        private var topActivityRef: WeakReference<Activity>? = null
        private val activityStacks = LinkedList<String>()

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            topActivityRef?.clear()
            activityStacks.addLast(activity.javaClass.name)
            topActivityRef = WeakReference(activity)
            Logger.i("activities=$activityStacks")
        }

        override fun onActivityStarted(activity: Activity) {
            Logger.i(activity.javaClass.name)
            activityCount++
            if (activity is SplashActivity) {
                isColdStart = false // 冷启动状态结束
            }
        }

        override fun onActivityResumed(activity: Activity) {
            isBackground = false
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
            activityCount--
            if (activityCount == 0) {
                Logger.i("AppStatusTracker", "run in background")
                isBackground = true
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (activityStacks.last == activity.javaClass.name) {
                activityStacks.removeLast()
            } else {
                activityStacks.remove(activity.javaClass.name)
            }
            Logger.i("activities=$activityStacks")
        }

        val topActivity: Activity?
            get() = topActivityRef?.get()
    }

    private fun isLocalServer(): Boolean {
        return DEV_ENV.contains(BuildConfig.FLAVOR_env)
    }

    fun hasAgree(): Boolean {
        return !Config.get().isFirstOpenApp
    }

    fun resetRan() {
        ran = random.nextInt(100000).toString()
    }

    fun setAlias(id: String) {
        umInitConfig.pushManager?.setAlias(id)
    }

    fun deleteAlias() {
        umInitConfig.pushManager?.deleteAlias()
    }

    fun post(r: Runnable) {
        mainThreadHandler.post(r)
    }

    fun postDelay(r: Runnable, delayMillis: Int) {
        mainThreadHandler.postDelayed(r, delayMillis.toLong())
    }

    override fun isDebug(): Boolean {
       return isLocalServer()
    }

    val pushManager: PushManager?
        get() = umInitConfig.pushManager

    fun initSdkAfterAgreeLicense() {
        if (hasAgree()) {
            Utils.init(this)
            DeviceID.register(this)
            Oaid.init()
            // 强烈建议在应用对应的Application#onCreate()方法中调用，避免出现content为null的异常

            // 友盟正式初始化
            umInitConfig.init(this, AppRouter)
            GrowingAutotracker.get().setDataCollectionEnabled(true)
            // QQ官方sdk授权
            // Tencent.setIsPermissionGranted(true);

            if (ProcessUtils.isMainProcess()) {
                /**
                 * adEnabled 为 true 表示 SDK 需要获取广告追踪相关参数
                 * f32a09dc-3312-d43e-6583-62fac13f33ae 是通过移动安全联盟获取到的 oaid
                 */
                val configuration = Configuration.Builder()
                    .imei(DeviceID.getUniqueID(this))
                    .build()
                OpenInstall.init(this, configuration)
                getInstall(5000, null)
            }

            CrashReport.setIsDevelopmentDevice(instance, BuildConfig.DEBUG)
            val strategy = CrashReport.UserStrategy(instance)
            strategy.appChannel = Session.getInstance().channel
            strategy.deviceID = Session.getInstance().androidId
            strategy.deviceModel = Session.getInstance().model

            val packageName = packageName
            val processName = ProcessUtils.getCurrentProcessName()
            CrashReport.setUserId(User.get().id)
            // 增加空安全判断
            strategy.isUploadProcess = processName == null || processName == packageName
            CrashReport.initCrashReport(applicationContext, Constants.BUGLY_APP_ID, BuildConfig.DEBUG, strategy)
            FlashLoginManager.init()
        }
    }

    private fun initGrowingIO() {
        //GrowingIO埋点库初始化
        val sConfiguration = AutotrackConfiguration("2f313003f3995a31", "growing.${BuildConfig.GROWING_URL_SCHEME}")
            .setDataCollectionServerHost("https://napi.growingio.com/")
            .setDataSourceId("bd72cad30bb36645")
            .setDebugEnabled(BuildConfig.DEBUG)
            .setAndroidIdEnabled(true)
            .setChannel(Session.getInstance().channel)
            .setRequireAppProcessesEnabled(true)
            .setDataCollectionEnabled(hasAgree())
            .setAutotrack(false)
        GrowingAutotracker.startWithConfiguration(this, sConfiguration)
    }

    /**
     * 初始化加载刷新控件
     */
    private fun initSmartRefreshLayoutView() {
        ClassicsFooter.REFRESH_FOOTER_NOTHING = resources.getString(R.string.no_more_data)
        ClassicsFooter.REFRESH_FOOTER_FINISH = ""
        // 刷新头
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, _ -> CustomizedRefreshHeader(context) }
        // 加载尾
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, _ ->
            val footer = ClassicsFooter(context)
            footer.setTextSizeTitle(12f)
            footer
        }
    }

    companion object {
        @JvmStatic
        lateinit var instance: App
        const val TAG = "_goldenfruit_app"

        // 相当于 Java 的 static boolean
        var isColdStart = true

        private val DEV_ENV = listOf("develop", "preview")
    }
}