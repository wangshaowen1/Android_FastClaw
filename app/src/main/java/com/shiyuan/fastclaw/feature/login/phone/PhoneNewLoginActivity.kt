package com.shiyuan.fastclaw.feature.login.phone

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.blankj.utilcode.util.RegexUtils
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.topActivity
import com.shiyuan.fastclaw.common.utils.KeyboardUtils
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.ext.getBundleValue
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.manager.ShareManager
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.core.network.apis.request.SendSmsValidateCodeBody
import com.shiyuan.fastclaw.databinding.ActivityPhoneNewLoginBinding
import com.shiyuan.fastclaw.feature.login.LoginActivity
import com.shiyuan.fastclaw.feature.login.dialog.PhoneNewPolicyDialog
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.umeng.socialize.UMShareAPI
import com.umeng.socialize.bean.SHARE_MEDIA

/**
 * 新版手机号登录页
 *  @author：CaoWei
 *  @date：2026/6/30 15:50
 */
class PhoneNewLoginActivity : BaseViewModelActivity<ActivityPhoneNewLoginBinding, PhoneNewLoginViewModel>(),
    ShareManager.UmengLoginListener {

        private val checkedPolicy by lazy { intent.getBundleValue(EXTRA_CHECKED_POLICY, false) }

    companion object {
        /**
         * 打开新版手机号登录页。
         *
         * @param showToast 是否先提示用户需要登录
         * @param checkedPolicy 是否沿用上一页已勾选的协议状态
         */
        fun start(showToast: Boolean = false, checkedPolicy: Boolean = false) {
            if (showToast) {
                showToast(R.string.toast_login_please)
            }
            val activity = topActivity ?: return
            val intent = Intent(activity, PhoneNewLoginActivity::class.java)
            intent.putExtra(EXTRA_CHECKED_POLICY, checkedPolicy)
            activity.startActivity(intent)
            EventManager.onAppTrack(AppEvent.KLX_CAPP_LOGIN_PAGE)
        }

        private const val EXTRA_CHECKED_POLICY = "checked_policy"
    }

    private val shareManager by lazy { ShareManager(this) }

    /**
     * 设置登录页的沉浸式状态栏样式。
     */
    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.login_bg)
            .transparentStatusBar()
            .init()
    }

    /**
     * 初始化页面状态、协议文案、输入框监听和点击事件。
     */
    override fun onViewModelCreated() {
        super.onViewModelCreated()
        mRealVM.checkedPolicy.set(checkedPolicy)
        initAgreement()
        initInput()
        initClicks()
        observeVm()
        showKeyboard()
    }

    /**
     * 监听手机号输入，实时同步 ViewModel 并更新错误提示。
     */
    private fun initInput() {
        binding.etPhone.doAfterTextChanged {
            mRealVM.mobile.value = it?.toString()
            updatePhoneInputStyle()
            mRealVM.checkSendCodeEnable()
            updatePhoneError(showOnlyWhenComplete = false)
        }
        mRealVM.mobile.value = binding.etPhone.text?.toString()
        updatePhoneInputStyle()
        mRealVM.checkSendCodeEnable()
    }

    /**
     * 按设计区分手机号占位态和已输入态的字重。
     */
    private fun updatePhoneInputStyle() {
        val isEmpty = binding.etPhone.text.isNullOrEmpty()
        binding.etPhone.typeface = if (isEmpty) {
            Typeface.DEFAULT
        } else {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        binding.ivClearPhone.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    /**
     * 进入页面后聚焦手机号输入框并弹出软键盘。
     */
    private fun showKeyboard() {
        KeyboardUtils.showKeyboard(binding.etPhone)
    }

    /**
     * 绑定返回、协议勾选、获取验证码和微信登录点击事件。
     */
    private fun initClicks() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.ivClearPhone.setOnClickListener {
            binding.etPhone.text?.clear()
            showKeyboard()
        }
        binding.llAgreement.setOnClickListener {
            mRealVM.checkedPolicy.set(!mRealVM.checkedPolicy.get())
        }
        binding.btnGetCode.setOnClickListener {
            requestCodeWithPolicy()
        }
        binding.btnWechat.setOnClickListener {
            loginByWechatWithPolicy()
        }
    }

    /**
     * 观察 ViewModel 状态并刷新按钮可点击态。
     */
    private fun observeVm() {
        mRealVM.sendCodeEnable.observe(this) {
            binding.btnGetCode.isEnabled = it
            binding.btnGetCode.alpha = if (it) 1f else 0.35f
        }
    }

    /**
     * 获取验证码前校验手机号和协议，未同意协议时先展示新版协议弹窗。
     */
    private fun requestCodeWithPolicy() {
        if (!updatePhoneError(showOnlyWhenComplete = true)) return
        if (mRealVM.checkedPolicy.get()) {
            requestCode()
            return
        }
        val dialog = PhoneNewPolicyDialog.show(supportFragmentManager)
        dialog.callback = callback@{ agree ->
            if (!agree) return@callback
            mRealVM.checkedPolicy.set(true)
            requestCode()
        }
    }

    /**
     * 请求短信验证码，成功后进入验证码登录页。
     */
    private fun requestCode() {
        val phone = binding.etPhone.text?.toString().orEmpty()
        mRealVM.mobile.value = phone
        mRealVM.sendSmsCode(
            startCountdownOnSuccess = false,
            bizType = SendSmsValidateCodeBody.BIZ_TYPE_LOGIN
        ) {
            PhoneNewCodeLoginActivity.start(this, phone, checkedPolicy = mRealVM.checkedPolicy.get())
            finish()
        }
    }

    /**
     * 微信登录前校验协议，未同意协议时先展示新版协议弹窗。
     */
    private fun loginByWechatWithPolicy() {
        if (!isWechatInstalled()) return
        if (mRealVM.checkedPolicy.get()) {
            startWechatLogin()
            return
        }
        val dialog = PhoneNewPolicyDialog.show(supportFragmentManager)
        dialog.callback = callback@{ agree ->
            if (!agree) return@callback
            mRealVM.checkedPolicy.set(true)
            startWechatLogin()
        }
    }

    /**
     * 检测微信客户端是否已安装，未安装时展示设计图提示。
     */
    private fun isWechatInstalled(): Boolean {
        val installed = UMShareAPI.get(this).isInstall(this, SHARE_MEDIA.WEIXIN)
        if (!installed) {
            showToast(R.string.wechat_client_not_found)
        }
        return installed
    }

    /**
     * 拉起微信授权。
     */
    private fun startWechatLogin() {
        shareManager.login(SHARE_MEDIA.WEIXIN, this)
    }

    /**
     * 刷新手机号错误提示。
     *
     * @param showOnlyWhenComplete true 表示点击提交时强制校验；false 表示输入满 11 位后才提示
     * @return 当前手机号是否合法
     */
    private fun updatePhoneError(showOnlyWhenComplete: Boolean): Boolean {
        val phone = binding.etPhone.text?.toString().orEmpty()
        val valid = RegexUtils.isMobileSimple(phone)
        val showError = if (showOnlyWhenComplete) !valid else phone.length >= 11 && !valid
        binding.tvPhoneError.visibility = if (showError) View.VISIBLE else View.INVISIBLE
        return valid
    }

    /**
     * 初始化协议富文本，给用户协议和隐私政策增加点击跳转。
     */
    private fun initAgreement() {
        val prefix = getString(R.string.login_agreement_prefix)
        val userAgreement = getString(R.string.login_user_agreement)
        val suffix = getString(R.string.login_agreement_suffix)
        val privacyPolicy = getString(R.string.login_privacy_policy)
        val text = SpannableString(prefix + " " + userAgreement + suffix + privacyPolicy)

        addAgreementSpan(
            text,
            text.indexOf(userAgreement),
            userAgreement.length,
            Config.get().serviceUrl,
            R.string.tvSYUserService
        )
        addAgreementSpan(
            text,
            text.indexOf(privacyPolicy),
            privacyPolicy.length,
            Config.get().privacyUrl,
            R.string.tvSYPrivateService
        )
        binding.tvAgreement.movementMethod = LinkMovementMethod.getInstance()
        binding.tvAgreement.highlightColor = Color.TRANSPARENT
        binding.tvAgreement.text = text
    }

    /**
     * 给协议文案的指定区间增加点击样式和跳转事件。
     */
    private fun addAgreementSpan(
        text: SpannableString,
        start: Int,
        length: Int,
        url: String?,
        titleRes: Int
    ) {
        if (start < 0) return
        text.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    SimpleBrowserActivity.launch(this@PhoneNewLoginActivity, url, getString(titleRes))
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(this@PhoneNewLoginActivity, R.color.color_336fff)
                    ds.isUnderlineText = false
                }
            },
            start,
            start + length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * 友盟授权开始回调。
     */
    override fun onStart(platform: SHARE_MEDIA?) {
        Logger.d("onStart platform:$platform")
    }

    /**
     * 友盟授权成功回调，仅处理微信登录。
     */
    override fun onAuthed(platform: SHARE_MEDIA?, data: MutableMap<String, String>?) {
        Logger.d("onAuthed platform:$platform data:$data")
        val loginType = when (platform) {
            SHARE_MEDIA.WEIXIN -> LoginByThirdBody.LOGIN_TYPE_WECHAT
            SHARE_MEDIA.QQ -> LoginByThirdBody.LOGIN_TYPE_QQ
            else -> {
                return
            }
        }
        mRealVM.loginByThird(loginType, map = data, onLogin = { needBindMobile, userData ->
                if (needBindMobile && userData != null) {
                    PhoneNewBindActivity.start(this, userData)
                }
                finish()
            }
        )
    }

    /**
     * 友盟授权失败或取消回调。
     */
    override fun onError(e: Exception?, platform: SHARE_MEDIA?, onCancel: Boolean) {
        Logger.d("onError e:$e platform:$platform onCancel:$onCancel")
    }
}
