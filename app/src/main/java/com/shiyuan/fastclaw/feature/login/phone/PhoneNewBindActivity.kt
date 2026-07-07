package com.shiyuan.fastclaw.feature.login.phone

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.blankj.utilcode.util.RegexUtils
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.showConfirmPrompt
import com.shiyuan.fastclaw.common.utils.KeyboardUtils
import com.shiyuan.fastclaw.core.network.apis.request.CheckMobileBindStatusBody.Companion.SCENE_BIND_MOBILE
import com.shiyuan.fastclaw.core.network.apis.request.SendSmsValidateCodeBody
import com.shiyuan.fastclaw.core.network.apis.response.UserHomeData
import com.shiyuan.fastclaw.databinding.ActivityPhoneNewBindBinding

/**
 * 三方登录后绑定手机号页。
 *  @author：CaoWei
 *  @date：2026/7/2 15:10
 */
class PhoneNewBindActivity : BaseViewModelActivity<ActivityPhoneNewBindBinding, PhoneNewLoginViewModel>() {

    companion object {
        private const val EXTRA_USER_DATA = "user_data"

        /**
         * 打开三方登录后的绑定手机号页。
         *
         * @param activity 当前页面
         * @param userData 三方登录接口返回的临时用户信息
         */
        fun start(activity: Activity, userData: UserHomeData) {
            val intent = Intent(activity, PhoneNewBindActivity::class.java)
            intent.putExtra(EXTRA_USER_DATA, userData)
            activity.startActivity(intent)
        }
    }

    private val thirdLoginUserData by lazy {
        intent.getParcelableExtra<UserHomeData>(EXTRA_USER_DATA)
    }

    /**
     * 设置绑定手机号页的沉浸式状态栏样式。
     */
    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_f7faff)
            .transparentStatusBar()
            .init()
    }

    /**
     * 初始化输入框监听、点击事件和页面状态。
     */
    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initInput()
        initClicks()
        observeVm()
        showKeyboard()
    }

    /**
     * 监听手机号输入并刷新错误提示和按钮状态。
     */
    private fun initInput() {
        binding.etPhone.doAfterTextChanged {
            mRealVM.mobile.value = it?.toString()
            updatePhoneInputStyle()
            mRealVM.checkSendCodeEnable()
            updatePhoneError(showOnlyWhenComplete = false)
        }
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
     * 绑定返回和获取验证码点击事件。
     */
    private fun initClicks() {
        binding.ivBack.setOnClickListener { finish() }
        binding.ivClearPhone.setOnClickListener {
            binding.etPhone.text?.clear()
            showKeyboard()
        }
        binding.btnGetCode.setOnClickListener {
            requestCode()
        }
    }

    /**
     * 观察 ViewModel 状态并刷新获取验证码按钮。
     */
    private fun observeVm() {
        mRealVM.sendCodeEnable.observe(this) {
            binding.btnGetCode.isEnabled = it
            binding.btnGetCode.alpha = if (it) 1f else 0.35f
        }
    }

    /**
     * 发送绑定手机号验证码，成功后进入验证码页的绑定模式。
     */
    private fun requestCode() {
        if (!updatePhoneError(showOnlyWhenComplete = true)) return
        val phone = binding.etPhone.text?.toString().orEmpty()
        val userData = thirdLoginUserData ?: return
        mRealVM.mobile.value = phone
        mRealVM.checkMobileBindStatus(scene = SCENE_BIND_MOBILE, bindTicket = userData.bindTicket, onSuccess = { data ->
                if (data?.isSuccess() == true) {
                    sendBindMobileCode(phone, userData)
                } else if (data?.popupStyle == 1) {
                    data.message?.showToast()
                } else if (data?.popupStyle == 2) {
                    showDirectLoginDialog(data.message)
                }
            },
            onError = {
                showPhoneError(it)
            }
        )
    }

    /**
     * 手机号前置校验通过后发送绑定验证码。
     */
    private fun sendBindMobileCode(phone: String, userData: UserHomeData) {
        mRealVM.sendSmsCode(
            startCountdownOnSuccess = false,
            bizType = SendSmsValidateCodeBody.BIZ_TYPE_LOGIN
        ) {
            PhoneNewCodeLoginActivity.startForBindMobile(this, phone, userData)
            finish()
        }
    }

    private fun showDirectLoginDialog(message: String?) {
        showConfirmPrompt(
            tip = message,
            title = getString(R.string.prompt),
            cancelText = getString(R.string.cancel),
            confirmText = getString(R.string.login_direct)
        ) {
            PhoneNewLoginActivity.start()
            finish()
        }
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
        if (showError) {
            binding.tvPhoneError.text = getString(R.string.login_new_phone_error)
        }
        binding.tvPhoneError.visibility = if (showError) View.VISIBLE else View.INVISIBLE
        return valid
    }

    /**
     * 在手机号输入框底部展示接口返回的错误提示。
     */
    private fun showPhoneError(message: String?) {
        binding.tvPhoneError.text = message.takeUnless { it.isNullOrBlank() } ?: getString(R.string.login_new_phone_error)
        binding.tvPhoneError.visibility = View.VISIBLE
    }
}
