package com.shiyuan.fastclaw.feature.main

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.core.util.getOrDefault
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.fm.openinstall.OpenInstall
import com.fm.openinstall.listener.AppWakeUpAdapter
import com.fm.openinstall.model.AppData
import com.google.gson.Gson
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.setupTranslucentStatus
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.ext.switchFragment
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.permissions.requestRxPermissions
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.Events.ClawVoiceEvent.MSG_ACTION_CANCEL
import com.shiyuan.fastclaw.core.Events.ClawVoiceEvent.MSG_ACTION_DOWN
import com.shiyuan.fastclaw.core.Events.ClawVoiceEvent.MSG_ACTION_MOVE
import com.shiyuan.fastclaw.core.Events.ClawVoiceEvent.MSG_ACTION_UP
import com.shiyuan.fastclaw.core.Events.ClawVoiceEvent.MSG_ACTION_VOLUME
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.manager.UpgradeManager
import com.shiyuan.fastclaw.core.model.WakeUpEntity
import com.shiyuan.fastclaw.databinding.ActivityMainBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentMainFragment
import com.shiyuan.fastclaw.feature.mine.MineFragment
import com.shiyuan.fastclaw.feature.report.report.HomeReportFragment
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.router.AppRouter
import com.shiyuan.fastclaw.widget.VoiceRecordingView.Companion.VOICE_CANCEL_DISTANCE
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
        const val FROM_NEWER = "Newer"

        fun start(activity: Activity?, scheme: String?= null,flags:Int?= null) {
            if (activity == null) return
            val intent = Intent(activity, MainActivity::class.java)
            scheme?.let {
                intent.putExtra(KEY_SCHEME, scheme)
            }
            flags?.let {
                intent.flags = it
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

    private var voiceDownRawY = 0f

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
        // 配合修改导航栏颜色适配部分rom的底部手势提示线
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            // 获取系统栏（状态栏 + 导航栏）的 Insets
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 动态设置 Padding：保持原有的左、上、右 Padding，仅改变底部 Padding
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, -systemBars.bottom)
            // 返回 insets 以便继续传递
            insets
        }

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
        //添加虾分等等
        mRealVM.commonInit()
        wakeUpAdapter?.let {
            // 此处要调用，否则App在后台运行时，会无法截获
            OpenInstall.getWakeUp(intent, it)
        }
        handleIntent(intent)
    }



    private fun initView() {
        binding.mrg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbHome -> {
                    //首页
                    EventManager.onAppTrack(AppEvent.KLX_CAPP_XIAOXIA_PAGE,mutableMapOf("klx_is_login" to if(User.get().isVisitor) "否" else "是"))
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(0)
                    supportFragmentManager.switchFragment(
                        R.id.fragment,
                        ClawAgentMainFragment::class.java
                    )
                }


//                R.id.rbWorks -> {
//                    //热门
//                    EventManager.onAppTrack(AppEvent.SF_VIEW_WORKS_TAB_PAGE)
//                    setKeepScreenOn(false)
//                    mRealVM.showTab.set(1)
//                    supportFragmentManager.switchFragment(
//                        R.id.fragment,
//                        HomeReportFragment::class.java
//                    )
//                }


                R.id.rbMine -> {
                    //我的
                    EventManager.onAppTrack(AppEvent.KLX_CAPP_MY_PAGE,mutableMapOf("klx_is_login" to if(User.get().isVisitor) "否" else "是"))
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(1)
                    supportFragmentManager.switchFragment(R.id.fragment, MineFragment::class.java)
                }
            }
        }
        when (Config.get().appConfig.openAppLoadTab) {
            0 -> binding.mrg.check(R.id.rbHome)
            1 -> binding.mrg.check(R.id.rbMine)
            else -> binding.mrg.check(R.id.rbHome)
        }
    }


    fun switchToTab(position:Int) {
        when(position){
            0 -> binding.mrg.check(R.id.rbHome)
            1 -> binding.mrg.check(R.id.rbMine)
            else -> binding.mrg.check(R.id.rbHome)
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

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.tabHome -> {
                clickTab(
                    tabId = R.id.tabHome,
                    null
                ) {
                    binding.mrg.check(R.id.rbHome) // 真正执行切换
                }
            }

            R.id.tabMine -> {
                clickTab(
                    tabId = R.id.tabMine,
                    null
                ) {
                    binding.mrg.check(R.id.rbMine) // 真正执行切换
                }
            }

            R.id.iv_main_report -> {
                binding.mrg.clearCheck()
                EventManager.onAppTrack(AppEvent.KLX_CAPP_INTERVIEWREPORT_LANDING_PAGE)
                setKeepScreenOn(false)
                mRealVM.showTab.set(2)
                supportFragmentManager.switchFragment(
                    R.id.fragment,
                    HomeReportFragment::class.java
                )
            }
        }
    }

    /**
     * @param tabId 对应 R.id
     * @param interceptor 拦截逻辑，返回 true 表示被拦截（不更新时间戳，不执行 action）
     * @param action 正常点击逻辑
     */
    private fun clickTab(
        tabId: Int,
        interceptor: (() -> Boolean)? = null,
        action: () -> Unit
    ) {
        val current = System.currentTimeMillis()
        val lastClick = clicks.getOrDefault(tabId, 0L)
        val diff = current - lastClick

        // 无论是否登录，短时间内（如 500ms）这段代码只会往下走一次
        if (diff < CLICK_INTERVAL_TIME) {
            when (tabId) {
                R.id.tabMine -> {
                    FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_MINE))
                }
                R.id.tabHome -> {
                    FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_HOME))
                }
                else -> {
                    FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_WORKS))
                }
            }
            return // 连续点击直接拦截，不再执行拦截器 check
        }

        // 只有过了防抖期，才更新时间，保证下一次连击被拦截
        clicks.put(tabId, current)

        if (interceptor?.invoke() == true) {
            return
        }

        // 4. 【执行切换】
        action()
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
        binding.mrg.check(R.id.rbHome)
    }


    override fun setTranslucentStatus() {
        setupTranslucentStatus(true, Color.WHITE)
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
            on<Events.MainTabEvent> { event ->
                when (event.type) {
                    Events.MainTabEvent.TAB_HOME -> {
                        toHome()
                    }

                    Events.MainTabEvent.TAB_MINE -> {
                        binding.mrg.check(R.id.rbMine)
                    }
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
            on<Events.ClawVoiceEvent> { events ->
                when(events.type) {
                    MSG_ACTION_DOWN -> {
                        voiceDownRawY = events.event.rawY
                        // 按下即开始录音；权限未授予时先拉起权限申请，本次触摸不进入录音态。
                        binding.layoutVoiceRecording.showRecording(canceling = false)
                    }
                    MSG_ACTION_MOVE-> {
                        // 以按下点为基准，上滑超过阈值进入取消态；滑回阈值内恢复发送态。
                        val voiceCanceling = voiceDownRawY - events.event.rawY > VOICE_CANCEL_DISTANCE
                        binding.layoutVoiceRecording.updateCanceling(voiceCanceling)
                    }
                    MSG_ACTION_UP -> {
                        binding.layoutVoiceRecording.hideRecording()
                    }
                    MSG_ACTION_CANCEL -> {
                        binding.layoutVoiceRecording.hideRecording()
                    }
                    MSG_ACTION_VOLUME -> {
                        binding.layoutVoiceRecording.updateVolume(events.rmsDb)
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
            }

            MainBulletFrameEnum.BIND_PHONE_NUMBER_POP_UP_BOX -> {
                //绑定手机号
            }
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
            if (mRealVM.showTab.get() == 1 && mRealVM.isEditing.get()) {
                //在我的页面如果是编辑状态，返回应该是修改状态
                mRealVM.setIsEditing?.invoke(true)
                return true
            }
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
