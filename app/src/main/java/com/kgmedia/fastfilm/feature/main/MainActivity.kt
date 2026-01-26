package com.kgmedia.fastfilm.feature.main

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.core.util.getOrDefault
import androidx.lifecycle.lifecycleScope
import com.fm.openinstall.OpenInstall
import com.fm.openinstall.listener.AppWakeUpAdapter
import com.fm.openinstall.model.AppData
import com.google.gson.Gson
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.showCommonDialog
import com.kgmedia.fastfilm.common.permissions.requestRxPermissions
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Events
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.UpgradeManager
import com.kgmedia.fastfilm.core.model.WakeUpEntity
import com.kgmedia.fastfilm.databinding.ActivityMainBinding
import com.kgmedia.fastfilm.feature.aicompose.generating.GeneratingViewModel
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.hit.HitFragment
import com.kgmedia.fastfilm.feature.login.SimpleProfileDialog
import com.kgmedia.fastfilm.feature.login.flash.PhoneLoginDialogActivity
import com.kgmedia.fastfilm.feature.mine.MineFragment
import com.kgmedia.fastfilm.feature.play.RecommendShortPlayPagerFragment
import com.kgmedia.fastfilm.router.AppNavigator
import com.kgmedia.fastfilm.router.AppRouter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.setupTranslucentStatus
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.ext.switchFragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2025/4/17  9:25
 * @Version: 1.0
 * *****************************************
 */
class MainActivity : BaseViewModelActivity<ActivityMainBinding, MainViewModel>() {
    companion object {
        val clicks = SparseArray<Long>()
        const val CLICK_INTERVAL_TIME: Long = 500
        private const val KEY_SCHEME = "KEY_SCHEME"

        fun start(activity: Activity?, scheme: String?) {
            if (activity == null) return
            val intent = Intent(activity, MainActivity::class.java)
            scheme?.let {
                intent.putExtra(KEY_SCHEME, scheme)
            }
            activity.startActivity(intent)
        }
    }

    private var exitTime: Long = 0
    private var needRefreshUserInfo: Boolean = false
    private val upgradeManager by lazy { UpgradeManager(this) }

    //当前用户id
    private var userId: String? = null

    //当前打开的时间，用与跟注册时间对比来判断这一次登录的是否是刚注册的账号
    private var registerTime: Long = 0

    //OpenInstall获取到的值
    private var wakeUpAdapter: AppWakeUpAdapter? = object : AppWakeUpAdapter() {
        override fun onWakeUp(appData: AppData) {
            //获取渠道数据
            //val channelCode = appData.getChannel()
            //获取绑定数据
            val bindData = appData.getData()
            val wakeUpEntity: WakeUpEntity? = Gson().fromJson(bindData, WakeUpEntity::class.java)
            if (wakeUpEntity?.page != null) {
                AppRouter.jump(wakeUpEntity.page)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerTime = System.currentTimeMillis()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                dalvik.system.ZipPathValidator.clearCallback()
            } catch (e: Exception) {
            }
        }
        userId = User.get().id
        //初始化
        onAgreePolicy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (User.get().requestNotificationsPermissionEnable) {
                binding.root.postDelayed({
                    requestRxPermissions(
                        R.string.request_notification_permission_reason.getResString(),
                        POST_NOTIFICATIONS
                    ) { granted ->
                        if (!granted) {
                            User.get().setRequestNotificationsPermissionDisable()
                        }
                    }
                }, 1200)
            }
        }
        mRealVM.startCreating.observe(this) {
            if (it) {
                toHome()
                mRealVM.startCreating.value = false
            }
        }
    }


    /**
     * 遍历查询是否初始化完成
     */
    private fun waitApiSecurityInit() {
        MainScope().launchNoCrash {
            while (!App.instance.isApiSecurityInit) {
                delay(100)
            }
            initialization()
            mRealVM.initialization()
        }
    }

    /**
     * 初始化
     */
    private fun initialization() {
        observeEvents()
        initView()
        //App检测更新
        checkUpgrade()
        wakeUpAdapter?.let {
            // 此处要调用，否则App在后台运行时，会无法截获
            OpenInstall.getWakeUp(intent, it)
        }
        handleIntent(intent)
    }

    private fun initView() {
        binding.mrg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.tabHome -> {
                    //首页
                    EventManager.onAppTrack(AppEvent.JG_VIEW_HOME_TAB_PAGE)
                    setKeepScreenOn(true)
                    mRealVM.showTab.set(0)
                    supportFragmentManager.switchFragment(
                        R.id.fragment,
                        RecommendShortPlayPagerFragment::class.java
                    )
                    lifecycleScope.launch {
                        delay(100)
                        displayPopUpBox(MainBulletFrameEnum.COMPLETE_PERSONAL_INFO)
                    }
                }

                R.id.tabWorks -> {
                    //热门
                    EventManager.onAppTrack(AppEvent.JG_VIEW_FADDISH_TAB_PAGE)
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(1)
                    supportFragmentManager.switchFragment(R.id.fragment, HitFragment::class.java)
                }


                R.id.tabMine -> {
                    //我的
                    EventManager.onAppTrack(AppEvent.JG_VIEW_MY_TAB_PAGE)
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(2)
                    supportFragmentManager.switchFragment(R.id.fragment, MineFragment::class.java)
                }
            }
        }
        when (Config.get().appConfig.openAppLoadTab) {
            0 -> binding.mrg.check(R.id.tabHome)
            1 -> binding.mrg.check(R.id.tabWorks)
            2 -> binding.mrg.check(R.id.tabMine)
            else -> binding.mrg.check(R.id.tabHome)
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (App.instance.hasAgree()) {
            wakeUpAdapter?.let {
                // 此处要调用，否则App在后台运行时，会无法截获
                OpenInstall.getWakeUp(intent, it)
            }
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.hasExtra(KEY_SCHEME)) {
            val schemeUri = intent.getStringExtra(KEY_SCHEME)
            AppRouter.jump(schemeUri)
        }
    }

    override fun onStart() {
        super.onStart()
        if (needRefreshUserInfo) {
            mRealVM.fetchUserInfo()
            needRefreshUserInfo = false
        }
    }

    override fun onPause() {
        super.onPause()
        GeneratingViewModel.visible = false
    }

    override fun onResume() {
        super.onResume()
        GeneratingViewModel.visible = false
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.tabHome -> {
                clickTab(tabId = R.id.tabHome) { diff ->
                    if (diff < CLICK_INTERVAL_TIME) {
                        FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_HOME))
                        true
                    } else false
                }
            }

            R.id.tabWorks -> {
                clickTab(tabId = R.id.tabWorks) { diff ->
                    if (diff < CLICK_INTERVAL_TIME) {
                        FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_HOT))
                        true
                    } else false
                }
            }

            R.id.tabMine -> {
                clickTab(tabId = R.id.tabMine) { diff ->
                    if (diff < CLICK_INTERVAL_TIME) {
                        FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_MINE))
                        true
                    } else false
                }
            }

            R.id.btnGenerateContinue -> {
                mRealVM.generating.get()?.let {
                    StoryEditorActivity.start(this, step = 1, dramaId = it.dramasId, scriptContent = it.scriptContent)
                }
            }

            R.id.ivGenerateClose -> {
                showCommonDialog("关闭后不再保留故事内容，请注意") {
                    mRealVM.closeGeneratingDramas()
                }
            }
        }
    }

    private fun clickTab(tabId: Int, clickAction: (diff: Long) -> Boolean) {
        val current = System.currentTimeMillis()

        if (!clickAction(current - clicks.getOrDefault(tabId, 0))) {
            clicks.put(tabId, current)
        }
    }

    // 添加常亮状态管理
    private var isScreenOnKeep = false

    /**
     * 设置屏幕常亮状态
     */
    private fun setKeepScreenOn(keepOn: Boolean) {
        if (keepOn == isScreenOnKeep) return

        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            isScreenOnKeep = true
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            isScreenOnKeep = false
        }
    }

    /**
     * 跳转到首页页面
     */
    fun toHome() {
        binding.mrg.check(R.id.tabHome)
    }


    override fun setTranslucentStatus() {
        setupTranslucentStatus(false, getThemeColor(R.attr.bgColor2))
    }

    private fun checkUpgrade() {
        lifecycleScope.launchNoCrash {
            delay(1000)
            upgradeManager.upgrade(false)
        }
    }

    private fun observeEvents() {
        observeEvents {
            on<Events.ApkRenewal> { event ->
                //用户日活记录
                mRealVM.daily()
                if (event.apkRenewal) {
                    //需要更新apk，那其他弹框就不需要弹了
                    return@on
                }
            }
            on<Events.UserChangedEvent> { event ->
                if (event.eventType == Events.UserChangedEvent.TOKEN_EXPIRED) {
                    AppNavigator.forceLogoutAndRestart()
                } else {
                    mRealVM.fetchUserInfo()
                    if (User.get().isLogged) {
                        //displayPopUpBox(type = MainBulletFrameEnum.COMPLETE_PERSONAL_INFO)
                    } else {
                        lifecycleScope.launch {
                            delay(100)
                            if (isActive) {
                                toHome()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 弹框管理
     * @param type MainBulletFrameEnum 弹框类型
     * @param nextStep Boolean 是否需要下一步
     * @param start Boolean true 首次启动的
     */
    private fun displayPopUpBox(
        type: MainBulletFrameEnum,
        nextStep: Boolean = true,
        start: Boolean = false
    ) {
        when (type) {
            MainBulletFrameEnum.DEFAULT -> return

            MainBulletFrameEnum.COMPLETE_PERSONAL_INFO -> {
                completePersonalInfo()
            }

            MainBulletFrameEnum.BIND_PHONE_NUMBER_POP_UP_BOX -> {
                //绑定手机号
                bindMobilePhone()
            }
        }
    }

    private fun completePersonalInfo(
        nextStep: Boolean = true,
        start: Boolean = false
    ) {
        SimpleProfileDialog.requireShow {
            "登录成功，开始创作吧~".showToast()
        }
    }


    private fun bindMobilePhone() {
        if (User.get().isVisitor) return
        if (!User.get().phoneBind.isNullOrEmpty()) return
        //获取用户注册时间
        val createTime = User.get().creationTime
        if (registerTime < createTime) {
            //是新注册的，不显示绑定手机
            return
        }
        //没有手机绑定，跳转到绑定手机页面
        PhoneLoginDialogActivity.start(activity = this, isBind = true, checkedPolicy = true)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            if (System.currentTimeMillis() - exitTime > 2000) {
                getString(R.string.press_again_to_exit).showToast()
                exitTime = System.currentTimeMillis()
            } else {
                finishAffinity()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    fun onAgreePolicy() {
        Config.get().markAppOpenedWithNotify()
        App.instance.initSdkAfterAgreeLicense()
        //遍历查询是否初始化完成
        waitApiSecurityInit()
    }
}