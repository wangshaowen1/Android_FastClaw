package com.shiyuan.fastclaw.feature.login.phone

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.databinding.ActivityPhoneNewCodeLoginBinding

/**
 * 新版验证码登录页
 */
class PhoneNewCodeLoginActivity : BaseViewModelActivity<ActivityPhoneNewCodeLoginBinding, PhoneNewLoginViewModel>() {

    companion object {
        private const val EXTRA_PHONE = "phone"
        private const val EXTRA_CHECKED_POLICY = "checked_policy"

        /**
         * 打开新版验证码登录页。
         *
         * @param phone 已发送验证码的手机号
         * @param checkedPolicy 协议勾选状态
         */
        fun start(context: Context, phone: String, checkedPolicy: Boolean) {
            val intent = Intent(context, PhoneNewCodeLoginActivity::class.java)
            intent.putExtra(EXTRA_PHONE, phone)
            intent.putExtra(EXTRA_CHECKED_POLICY, checkedPolicy)
            context.startActivity(intent)
        }
    }

    private val codeViews by lazy {
        listOf(binding.tvCode1, binding.tvCode2, binding.tvCode3, binding.tvCode4)
    }
    private var isLoggingIn = false

    /**
     * 设置验证码页的沉浸式状态栏样式。
     */
    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.login_bg)
            .transparentStatusBar()
            .init()
    }

    /**
     * 初始化手机号、验证码输入框、重发倒计时和软键盘。
     */
    override fun onViewModelCreated() {
        super.onViewModelCreated()
        val phone = intent.getStringExtra(EXTRA_PHONE).orEmpty()
        mRealVM.mobile.value = phone
        mRealVM.checkedPolicy.set(intent.getBooleanExtra(EXTRA_CHECKED_POLICY, true))
        binding.tvCodeSubtitle.text = getString(R.string.login_new_code_subtitle, phone)
        initInput()
        initClicks()
        observeVm()
        updateCodeBoxes("")
        updateResendText()
        mRealVM.startCountdown()
        binding.etCode.post { showCodeKeyboard() }
    }

    /**
     * 监听验证码输入，满 4 位后自动发起手机号登录。
     */
    private fun initInput() {
        binding.etCode.doAfterTextChanged {
            val code = it?.toString().orEmpty()
            mRealVM.code.value = code
            binding.tvCodeError.visibility = View.GONE
            updateCodeBoxes(code)
            if (code.length == Constants.SMS_CODE_LENGTH && !isLoggingIn) {
                loginByPhone()
            }
        }
    }

    /**
     * 绑定返回、验证码区域唤起键盘和重新获取验证码事件。
     */
    private fun initClicks() {
        binding.ivBack.setOnClickListener { finish() }
        binding.llCodeBoxes.setOnClickListener { showCodeKeyboard() }
        binding.etCode.setOnClickListener { showCodeKeyboard() }
        binding.tvResend.setOnClickListener {
            if (mRealVM.codeCounting.value == true) return@setOnClickListener
            mRealVM.sendSmsCode {
                binding.etCode.setText("")
            }
        }
    }

    /**
     * 观察倒计时状态，刷新重新获取验证码文案。
     */
    private fun observeVm() {
        mRealVM.codeCounting.observe(this) {
            updateResendText()
        }
        mRealVM.sendCodeText.addOnPropertyChangedCallback(object :
            androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(
                sender: androidx.databinding.Observable?,
                propertyId: Int
            ) {
                updateResendText()
            }
        })
    }

    /**
     * 使用手机号和验证码登录，失败时显示验证码错误提示。
     */
    private fun loginByPhone() {
        isLoggingIn = true
        mRealVM.phoneLogin(
            onLogin = {
                finish()
            },
            onError = {
                isLoggingIn = false
                binding.tvCodeError.visibility = View.VISIBLE
            }
        )
    }

    /**
     * 聚焦隐藏输入框并弹出数字键盘。
     */
    private fun showCodeKeyboard() {
        binding.etCode.requestFocus()
        getSystemService<InputMethodManager>()?.showSoftInput(
            binding.etCode,
            InputMethodManager.SHOW_IMPLICIT
        )
    }

    /**
     * 根据输入内容刷新 4 个验证码展示框。
     */
    private fun updateCodeBoxes(code: String) {
        codeViews.forEachIndexed { index, textView ->
            textView.text = code.getOrNull(index)?.toString().orEmpty()
        }
    }

    /**
     * 根据倒计时状态刷新重新获取验证码文案和透明度。
     */
    private fun updateResendText() {
        val counting = mRealVM.codeCounting.value == true
        val rawText = mRealVM.sendCodeText.get().orEmpty()
        binding.tvResend.text = if (counting && rawText.endsWith("s")) {
            getString(R.string.login_new_resend_countdown, rawText.removeSuffix("s"))
        } else {
            getString(R.string.resend_sms_code)
        }
        binding.tvResend.setTextColor(
            ContextCompat.getColor(
                this,
                if (counting) R.color.color_55_3c3c43 else R.color.color_2f6bff
            )
        )
    }
}
