package com.shiyuan.fastclaw.core.manager

import android.graphics.Color
import android.view.LayoutInflater
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
    private var listener: WeakReference<Listener>? = null
    private var binding: LayoutShanyanCustomBinding? = null
    val checkedPolicy
        get() = OneKeyLoginManager.getInstance().privacyCheckBox?.isChecked == true

    fun checkPolicy(checked: Boolean) {
        OneKeyLoginManager.getInstance().setCheckBoxValue(checked)
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
        ShanYanUIConfig.Builder()
            .setTextSizeIsdp(true)
            //背景
            .setAuthBGImgPath(R.color.transparent.getResDrawable())
            //状态栏
            .setLightColor(true)
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
            .setPrivacyTextSize(11)
            .setAppPrivacyColor(R.color.color_90_000000.getResColor(), R.color.color_2f6bff.getResColor())
            .setPrivacyOffsetX(24)
            .setPrivacyOffsetY(185)
            .setPrivacyState(false)
            .setCheckedImgPath(R.drawable.ic_flash_new_check_checked.getResDrawable())
            .setUncheckedImgPath(R.drawable.ic_flash_new_check_normal.getResDrawable())
            .setCheckBoxWH(14, 14)
            .setcheckBoxOffsetXY(0, 6)
            .setCheckBoxMargin(0, 0, 0, 0)
            .setCheckBoxTipDisable(true)
            .setPrivacyText("已阅读并同意", "、", "以及", "", "")
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

    /**
     * 同意协议
     */
    @JvmStatic
    fun agreePolicy() {
        OneKeyLoginManager.getInstance().setCheckBoxValue(true)
    }

    /**
     * 同意协议并登录
     */
    @JvmStatic
    fun agreePolicyAndLogin() {
        OneKeyLoginManager.getInstance().setCheckBoxValue(true)
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
