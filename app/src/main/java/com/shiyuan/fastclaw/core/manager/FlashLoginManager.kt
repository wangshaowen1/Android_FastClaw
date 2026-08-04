package com.shiyuan.fastclaw.core.manager

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.SizeUtils
import com.chuanglan.shanyan_sdk.OneKeyLoginManager
import com.chuanglan.shanyan_sdk.tool.ShanYanUIConfig
import com.nhsd.basic.ext.dp2px
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.utils.Logger
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.BuildConfig
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.getScreenWidth
import com.shiyuan.fastclaw.common.ext.topActivity
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.databinding.singleClick
import com.shiyuan.fastclaw.databinding.LayoutShanyanCustomBinding
import com.nhsd.basic.ext.dp
import com.nhsd.crop.util.ViewUtils
import com.umeng.socialize.bean.SHARE_MEDIA
import org.json.JSONObject
import java.lang.ref.WeakReference

/**
 * 闪验登录
 * @author xiaoowuu
 */
object FlashLoginManager {

    private const val TAG = "FlashLoginManager"
    private const val SHANYAN_AUTH_ACTIVITY_CLASS = "com.chuanglan.shanyan_sdk.view.ShanYanOneKeyActivity"
    private const val SHANYAN_AUTH_ACTIVITY_REF_FIELD = "mLoginActivityWeakReference"
    private var listener: WeakReference<Listener>? = null
    private var binding: LayoutShanyanCustomBinding? = null
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    val checkedPolicy
        get() = OneKeyLoginManager.getInstance().privacyCheckBox?.isChecked == true

    fun checkPolicy(checked: Boolean) {
        OneKeyLoginManager.getInstance().setCheckBoxValue(checked)
        updateCustomPolicyCheckView()
    }

    fun setListener(listener: Listener) {
        FlashLoginManager.listener = WeakReference(listener)
        OneKeyLoginManager.getInstance().setPrivacyOnClickListener { url: String?, name: String? -> FlashLoginManager.listener?.get()?.toPolicy(url, name) }
        OneKeyLoginManager.getInstance().setActionListener { type, code, message ->
            //type=3 ，一键登录点击事件，code 分为 0,1（0 为协议未勾选时，1 为协议勾选时）
            if (type == 3 && code == 0) {
                FlashLoginManager.listener?.get()?.showPolicyDialog()
            } else if (type == 2 && code == 1) {
                //type=2 ，checkbox 点击事件，code 分为 0,1（0 为未选中，1 为选中）
            }
        }
    }

    fun removeListener() {
        binding = null
        listener?.clear()
        listener = null
        OneKeyLoginManager.getInstance().removeAllListener()
    }

    /**
     * UI配置
     * @param onOtherLogin 点击其他登录回调 type 1:手机号 2:QQ 3:微信
     */
    private fun uiConfig(onCancel: () -> Unit, onOtherLogin: (type: Int) -> Unit) = run {
        val screenWidth = ScreenUtils.getScreenWidth()
        val width = SizeUtils.px2dp(screenWidth.toFloat())
        val navHeight = topActivity?.let { SizeUtils.px2dp(ViewUtils.getNavigationBarHeight(it).toFloat()) }
        val height = width / 375f * (337 + (navHeight ?: 0))
        val customView = LayoutShanyanCustomBinding.inflate(LayoutInflater.from(App.instance))
        binding = customView
        binding?.bg?.layoutParams?.height = height.toInt().dp
        customView.container.layoutParams = LinearLayout.LayoutParams(screenWidth, height.toInt().dp)
        singleClick(customView.btnClose) {
            finishAuthActivity()
            onCancel.invoke()
        }
        //手机号登录
        singleClick(customView.ivLoginMobilePhoneNumber) { onOtherLogin.invoke(1) }
        singleClick(customView.ivLoginWx) { onOtherLogin.invoke(3) }
        setupCustomPolicyView(customView)
        ShanYanUIConfig.Builder()
            .setTextSizeIsdp(true)
            //背景
            .setAuthBGImgPath(R.color.transparent.getResDrawable())
            //状态栏
            .setLightColor(true)
            .setVirtualKeyTransparent(true)
            .setFitsSystemWindows(false)
            //导航栏（默认高度：50dp）
            .setAuthNavHidden(true)
            //logo图标
            .setLogoHidden(true)
            //号码栏
            .setNumberColor(R.color.color_96_000000.getResColor())
            .setNumFieldOffsetY(27)
            .setNumberSize(24)
            .setNumberBold(true)
            //登录按钮（默认高度：46dp）
            .setLogBtnImgPath(R.drawable.bg_2f6bff_r100.getResDrawable())
            .setLogBtnText(R.string.sim_card_quick_login.getResString())
            .setLogBtnTextSize(15)
            .setLogBtnTextColor(R.color.white.getResColor())
            .setLogBtnTextBold(true)
            .setLogBtnWidth(327)
            .setLogBtnHeight(48)
            .setLogBtnOffsetY(81)
            //其他登录
            .addCustomView(customView.root, false, false, null)
            //隐私栏
            .setAppPrivacyOne(R.string.tvSYUserService.getResString(), Config.get().serviceUrl)
            .setAppPrivacyTwo(R.string.tvSYPrivateService.getResString(), Config.get().privacyUrl)
            .setPrivacySmhHidden(false)
            .setPrivacyTextSize(1)
            .setAppPrivacyColor(R.color.transparent.getResColor(), R.color.transparent.getResColor())
            //隐藏了自带的隐私协议，原因是协议在部分手机显示不全，使用布局xml的隐私协议
            .setPrivacyWidth(1)
            .setPrivacyOffsetX(0)
            .setPrivacyOffsetY(1000)
            .setPrivacyState(false)
            .setCheckedImgPath(R.drawable.ic_flash_new_check_checked.getResDrawable())
            .setUncheckedImgPath(R.drawable.ic_flash_new_check_normal.getResDrawable())
            .setCheckBoxWH(14, 14)
            .setcheckBoxOffsetXY(0, 6)
            .setCheckBoxMargin(0, 0, 0, 0)
            .setCheckBoxHidden(true)
            .setCheckBoxTipDisable(true)
            .setPrivacyText("", "", "", "", "")
            .setPrivacyGravityHorizontalCenter(false)
            .setPrivacyOffsetGravityLeft(true)
            .setPrivacyActivityEnabled(false)
            .setOperatorPrivacyAtLast(true)
            .setPrivacyTextBold(false)
            //运营商slogan（**提供认证服务）
            .setSloganHidden(true)
            //底部弹窗
            .setDialogTheme(true, width, height.toInt(), 0, 0, true)
            .build()
    }

    /**
     * 初始化自定义协议栏，负责勾选状态切换、协议富文本展示和链接点击。
     */
    private fun setupCustomPolicyView(customView: LayoutShanyanCustomBinding) {
        updateCustomPolicyCheckView()
        singleClick(customView.layoutPolicy) {
            checkPolicy(!checkedPolicy)
        }
        singleClick(customView.ivPolicyCheck) {
            checkPolicy(!checkedPolicy)
        }
        customView.tvPolicyText.text = buildPolicyText()
        customView.tvPolicyText.movementMethod = LinkMovementMethod.getInstance()
        customView.tvPolicyText.highlightColor = Color.TRANSPARENT
    }

    /**
     * 组装一键登录协议文案，并为平台协议、隐私政策和运营商协议添加点击区域。
     */
    private fun buildPolicyText(): SpannableString {
        val serviceName = R.string.tvSYUserService.getResString()
        val privacyName = R.string.tvSYPrivateService.getResString()
        val operatorName = operatorPolicyName.takeUnless { it.isNullOrBlank() }
            ?: R.string.flash_new_account_terms.getResString()
        val text = SpannableString("已阅读并同意《$serviceName》、《$privacyName》以及《$operatorName》")
        addPolicySpan(text, serviceName, Config.get().serviceUrl)
        addPolicySpan(text, privacyName, Config.get().privacyUrl)
        addPolicySpan(text, operatorName, operatorPolicyUrl)
        return text
    }

    /**
     * 给协议文案中的指定协议名称添加蓝色点击样式和跳转事件。
     */
    private fun addPolicySpan(text: SpannableString, name: String, url: String?) {
        val start = text.indexOf(name)
        if (start < 0) return
        text.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    listener?.get()?.toPolicy(url, name)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = R.color.color_2f6bff.getResColor()
                    ds.isUnderlineText = false
                }
            },
            start,
            start + name.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * 根据闪验 SDK 当前协议勾选状态刷新自定义勾选图标。
     */
    private fun updateCustomPolicyCheckView() {
        binding?.ivPolicyCheck?.setImageResource(
            if (checkedPolicy) {
                R.drawable.ic_checkbox_checked
            } else {
                R.drawable.icon_checkbox_unchecked
            }
        )
    }

    /**
     * 初始化
     */
    fun init() {
        OneKeyLoginManager.getInstance().setDebug(BuildConfig.DEBUG)
        OneKeyLoginManager.getInstance().setTimeOutForPreLogin(3)
        OneKeyLoginManager.getInstance().init(App.instance, BuildConfig.SHANYAN_APPID) { code, result ->
            Logger.i(TAG, "init code=$code,result=$result")
        }
    }

    /**
     * 闪验登录
     * @param onOpen 授权页拉起成功
     * @param onOpenFail 授权页拉起失败
     * @param onLogin 登录成功回调token
     * @param onLoginFail 登录失败或其他登录
     * @param onOtherLogin 点击其他登录回调 type 1:手机号 2:QQ 3:微信
     */
    @JvmStatic
    fun login(
        onOpen: () -> Unit,
        onOpenFail: (code: Int, result: String?) -> Unit,
        onLogin: (token: String) -> Unit,
        onLoginFail: (code: Int, result: String?) -> Unit,
        onCancel: () -> Unit,
        onOtherLogin: (type: Int) -> Unit,
    ) {
        val uiConfig = uiConfig(onCancel, onOtherLogin)
        OneKeyLoginManager.getInstance().setAuthThemeConfig(uiConfig, uiConfig)
        OneKeyLoginManager.getInstance()
            .openLoginAuth(false, { code, result ->
                //getOpenLoginAuthStatus(int code,String result)
                //code为1000：授权页成功拉起 其他：失败
                Logger.i(TAG, "getOpenLoginAuthStatus($code, $result)")
                if (1000 == code) {
                    applyAuthWindowBehindNavigationBar()
                    onOpen.invoke()
                } else {
                    onOpenFail.invoke(code, result)
                }
            }) { code, result ->
                //getOneKeyLoginStatus(int code,String result)
                //code为1000：成功 其他：失败 (包含点击返回键 code==1011)
                //当外层code为1000时，result的返回为{"token":""}
                Logger.i(TAG, "getOneKeyLoginStatus($code, $result)")
                when (code) {
                    1000 -> runCatching { JSONObject(result).getString("token") }.getOrNull()
                        ?.let(onLogin) ?: onLoginFail.invoke(code, result)

                    1011 -> onCancel.invoke()
                    else -> onLoginFail.invoke(code, result)
                }
            }
    }

    private fun applyAuthWindowBehindNavigationBar() {
        runOnMainThread {
            setupAuthWindowBehindNavigationBar()
            mainHandler.postDelayed({ setupAuthWindowBehindNavigationBar() }, 80)
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun setupAuthWindowBehindNavigationBar() {
        val activity = getShanYanAuthActivity()
        if (activity == null) {
            Logger.i(TAG, "setupAuthWindowBehindNavigationBar auth activity is null")
            return
        }
        activity.window?.let(::setupWindowBehindNavigationBar)
        setupAuthRootFitsSystemWindows(activity)
    }

    private fun getShanYanAuthActivity(): Activity? {
        return runCatching {
            val activityClass = Class.forName(SHANYAN_AUTH_ACTIVITY_CLASS)
            val activityRefField = activityClass.getDeclaredField(SHANYAN_AUTH_ACTIVITY_REF_FIELD)
            activityRefField.isAccessible = true
            val activityRef = activityRefField.get(null) as? WeakReference<*>
            activityRef?.get() as? Activity
        }.onFailure {
            Logger.e(TAG, "getShanYanAuthActivity error:${it.stackTraceToString()}")
        }.getOrNull()
    }

    private fun setupWindowBehindNavigationBar(window: Window) {
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        var systemUiVisibility = window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.decorView.systemUiVisibility = systemUiVisibility
    }

    private fun setupAuthRootFitsSystemWindows(activity: Activity) {
        val rootId = activity.resources.getIdentifier(
            "shanyan_view_login_layout",
            "id",
            activity.packageName
        )
        if (rootId == 0) return
        activity.findViewById<View>(rootId)?.fitsSystemWindows = false
    }

    /**
     * 同意协议
     */
    @JvmStatic
    fun agreePolicy() {
        OneKeyLoginManager.getInstance().setCheckBoxValue(true)
        updateCustomPolicyCheckView()
    }

    /**
     * 同意协议并登录
     */
    @JvmStatic
    fun agreePolicyAndLogin() {
        OneKeyLoginManager.getInstance().setCheckBoxValue(true)
        updateCustomPolicyCheckView()
        OneKeyLoginManager.getInstance().performLoginClick()
    }

    /**
     * 关闭一键登录
     */
    @JvmStatic
    fun finishAuthActivity() {
        OneKeyLoginManager.getInstance().finishAuthActivity()
    }

    @JvmStatic
    fun hideLoading() {
        OneKeyLoginManager.getInstance().setLoadingVisibility(false)
    }

    /**
     * 运营商信息
     */
    private val operatorInfo
        get() = runCatching { JSONObject(OneKeyLoginManager.getInstance().getOperatorInfo(App.instance)) }.getOrNull()

    /**
     * 运营商协议标题
     */
    val operatorPolicyName
        get() = operatorInfo?.optString("protocolName")

    /**
     * 运营商协议url
     */
    val operatorPolicyUrl
        get() = operatorInfo?.optString("protocolUrl")

    interface Listener {
        /**
         * 跳转协议页
         * @param url 协议url
         * @param name 协议标题
         */
        fun toPolicy(url: String?, name: String?)

        /**
         * 闪验登录隐私弹窗
         */
        fun showPolicyDialog()
    }
}
