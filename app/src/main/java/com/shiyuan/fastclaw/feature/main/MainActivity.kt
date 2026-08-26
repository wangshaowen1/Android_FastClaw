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
import androidx.lifecycle.lifecycleScope
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.setupTranslucentStatus
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.ext.switchFragment
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.formatMessageNum
import com.shiyuan.fastclaw.common.permissions.requestRxPermissions
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.User
import com.shiyuan.fastclaw.core.manager.AdsManager
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.AppEventParam
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.manager.UpgradeManager
import com.shiyuan.fastclaw.databinding.ActivityMainBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentMainFragment
import com.shiyuan.fastclaw.feature.message.MessageFragment
import com.shiyuan.fastclaw.feature.mine.MineFragment
import com.shiyuan.fastclaw.feature.opportunity.HomeOpportunityFragment
import com.shiyuan.fastclaw.router.AppNavigator
import com.shiyuan.fastclaw.router.AppRouter
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
        //初始化  调用init方法，要求传入当前前台的Activity 所以放到 MainActivity (已经同意过隐私协议)
        AdsManager.initAdsReporter(this, BuildConfig.DEBUG)
        waitApiSecurityInit()
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
        initObserve()
        //App检测更新
        checkUpgrade()
        //添加虾分等等
        mRealVM.commonInit()
        handleIntent(intent)
    }



    private fun initView() {
        binding.mrg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbHome -> {
                    //首页
                    EventManager.onAppTrack(AppEvent.KLX_CAPP_XIAOXIA_PAGE, mutableMapOf(AppEventParam.KEY_IS_LOGIN to if (User.get().isVisitor) "否" else "是"))
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(0)
                    binding.vLine.visibility = View.GONE
                    supportFragmentManager.switchFragment(
                        R.id.fragment,
                        ClawAgentMainFragment::class.java
                    )
                }
                R.id.rbTreasury -> {
                    EventManager.onAppTrack(AppEvent.KLX_CAPP_INTERVIEWREPORT_LANDING_PAGE)
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(1)
                    binding.vLine.visibility = View.VISIBLE
                    supportFragmentManager.switchFragment(
                        R.id.fragment,
                        HomeOpportunityFragment::class.java
                    )
                }
                R.id.rbMessage -> {
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(2)
                    binding.vLine.visibility = View.VISIBLE
                    supportFragmentManager.switchFragment(
                        R.id.fragment,
                        MessageFragment::class.java
                    )
                }
                R.id.rbMine -> {
                    //我的
                    EventManager.onAppTrack(AppEvent.KLX_CAPP_MY_PAGE, mutableMapOf(AppEventParam.KEY_IS_LOGIN to if (User.get().isVisitor) "否" else "是"))
                    setKeepScreenOn(false)
                    mRealVM.showTab.set(3)
                    binding.vLine.visibility = View.VISIBLE
                    supportFragmentManager.switchFragment(R.id.fragment, MineFragment::class.java)
                }
            }
        }
        when (Config.get().appConfig.openAppLoadTab) {
            0 -> binding.mrg.check(R.id.rbHome)
            1 -> binding.mrg.check(R.id.rbTreasury)
            2 -> binding.mrg.check(R.id.rbMessage)
            3 -> binding.mrg.check(R.id.rbMine)
            else -> binding.mrg.check(R.id.rbHome)
        }
    }

    /**
     * 订阅主界面状态变化，保持底部 Tab 数量同步。
     */
    private fun initObserve() {
        mRealVM.imUnreadCount.observe(this) { count ->
            updateMessageCount(count)
        }
        mRealVM.opportunityUnprocessedCount.observe(this) { count ->
            updateOpportunityCount(count)
        }
    }

    /**
     * 更新消息 Tab 未读红点，0 条时隐藏，超过 99 条按 99 展示。
     */
    private fun updateMessageCount(count: Int) {
        binding.sbMessageCount.text = count.formatMessageNum()
        binding.sbMessageCount.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    /**
     * 更新机会 Tab 未处理数量，0 条时隐藏，超过 99 条按 99 展示。
     */
    private fun updateOpportunityCount(count: Int) {
        binding.sbOpportunityCount.text = count.formatMessageNum()
        binding.sbOpportunityCount.visibility = if (count > 0) View.VISIBLE else View.GONE
    }


    fun switchToTab(position:Int) {
        when(position){
            0 -> binding.mrg.check(R.id.rbHome)
            1 -> binding.mrg.check(R.id.rbTreasury)
            2 -> binding.mrg.check(R.id.rbMessage)
            3 -> binding.mrg.check(R.id.rbMine)
            else -> binding.mrg.check(R.id.rbHome)
        }

    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (App.instance.hasAgree()) {
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

            R.id.tabTreasury -> {
                clickTab(
                    tabId = R.id.tabTreasury,
                    null
                ) {
                    binding.mrg.check(R.id.rbTreasury) // 真正执行切换
                }
            }

            R.id.tabMessage -> {
                clickTab(
                    tabId = R.id.tabMessage,
                    null
                ) {
                    binding.mrg.check(R.id.rbMessage)
                }
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
                R.id.tabTreasury -> {
                    FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_TREASURY))
                }
                R.id.tabMessage -> {
                    FlowEventBus.postSync(Events.MainTabDoubleClickEvent(Events.MainTabDoubleClickEvent.TAB_MESSAGE))
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
        setupTranslucentStatus(true, Color.TRANSPARENT)
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

                    Events.MainTabEvent.TAB_MESSAGE -> {
                        binding.mrg.check(R.id.rbMessage)
                    }
                }
            }
            on<Events.OpportunityNavCountRefreshEvent> {
                mRealVM.queryOpportunityNavCount()
            }
            on<Events.UserChangedEvent> { event ->
                if (event.eventType == Events.UserChangedEvent.TOKEN_EXPIRED) {
                    mRealVM.clearImUnreadCount()
                    mRealVM.clearOpportunityNavCount()
                    AppNavigator.forceLogoutAndLogin()
                } else {
                    mRealVM.fetchUserInfo()
                    if (User.get().isLogged) {
                        mRealVM.refreshImData()
                        //displayPopUpBox(type = MainBulletFrameEnum.COMPLETE_PERSONAL_INFO)
                    } else {
                        mRealVM.clearImUnreadCount()
                        mRealVM.clearOpportunityNavCount()
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
}
