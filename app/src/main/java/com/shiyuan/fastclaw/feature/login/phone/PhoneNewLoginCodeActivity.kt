package com.shiyuan.fastclaw.feature.login.phone

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.Constants
import com.shiyuan.fastclaw.common.ext.toMaskedPhone
import com.shiyuan.fastclaw.common.utils.KeyboardUtils
import com.shiyuan.fastclaw.core.network.apis.request.SendSmsValidateCodeBody
import com.shiyuan.fastclaw.core.network.apis.response.UserHomeData
import com.shiyuan.fastclaw.databinding.ActivityPhoneNewCodeLoginBinding
import com.shiyuan.fastclaw.feature.login.dialog.LoginNewLoadingDialog
import com.shiyuan.fastclaw.feature.main.MainActivity

/**
 * 新版验证码登录页
 *  @author：CaoWei
 *  @date：2026/6/30 15:49
 */
class PhoneNewLoginCodeActivity :
    BaseViewModelActivity<ActivityPhoneNewCodeLoginBinding, PhoneNewLoginViewModel>() {

    private val codeViews by lazy {
        listOf(binding.tvCode1, binding.tvCode2, binding.tvCode3, binding.tvCode4)
    }
    private var loginLoadingDialog: LoginNewLoadingDialog? = null
    private var isLoggingIn = false
    private var bindUserData: UserHomeData? = null
    private var businessType = BUSINESS_TYPE_LOGIN

    companion object {
        private const val EXTRA_PHONE = "phone"
        private const val EXTRA_CHECKED_POLICY = "checked_policy"
        private const val EXTRA_BIND_USER_DATA = "bind_user_data"
        private const val EXTRA_BUSINESS_TYPE = "business_type"

        /**
         * 验证码业务类型：手机号验证码登录。
         */
        const val BUSINESS_TYPE_LOGIN = 1

        /**
         * 验证码业务类型：三方登录后绑定手机号。
         */
        const val BUSINESS_TYPE_BIND_PHONE = 2

        /**
         * 验证码业务类型：后续手机号换绑复用。
         */
        const val BUSINESS_TYPE_CHANGE_PHONE = 3

        /**
         * 打开新版验证码登录页。
         *
         * @param phone 已发送验证码的手机号
         * @param checkedPolicy 协议勾选状态
         */
        fun start(context: Context, phone: String, checkedPolicy: Boolean) {
            val intent = Intent(context, PhoneNewLoginCodeActivity::class.java)
            intent.putExtra(EXTRA_PHONE, phone)
            intent.putExtra(EXTRA_CHECKED_POLICY, checkedPolicy)
            intent.putExtra(EXTRA_BUSINESS_TYPE, BUSINESS_TYPE_LOGIN)
            context.startActivity(intent)
        }

        /**
         * 打开三方登录绑定手机号验证码页。
         *
         * @param context 当前页面
         * @param phone 已发送验证码的手机号
         * @param userData 三方登录接口返回的临时用户信息
         */
        fun startForBindMobile(context: Context, phone: String, userData: UserHomeData) {
            val intent = Intent(context, PhoneNewLoginCodeActivity::class.java)
            intent.putExtra(EXTRA_PHONE, phone)
            intent.putExtra(EXTRA_CHECKED_POLICY, true)
            intent.putExtra(EXTRA_BUSINESS_TYPE, BUSINESS_TYPE_BIND_PHONE)
            intent.putExtra(EXTRA_BIND_USER_DATA, userData)
            context.startActivity(intent)
        }

        /**
         * 打开修改手机号验证码页。
         *
         * @param context 当前页面
         * @param phone 已发送验证码的新手机号
         */
        fun startForChangePhone(context: Context, phone: String) {
            val intent = Intent(context, PhoneNewLoginCodeActivity::class.java)
            intent.putExtra(EXTRA_PHONE, phone)
            intent.putExtra(EXTRA_CHECKED_POLICY, true)
            intent.putExtra(EXTRA_BUSINESS_TYPE, BUSINESS_TYPE_CHANGE_PHONE)
            context.startActivity(intent)
        }
    }

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
        businessType = intent.getIntExtra(EXTRA_BUSINESS_TYPE, BUSINESS_TYPE_LOGIN)
        bindUserData = intent.getParcelableExtra(EXTRA_BIND_USER_DATA)
        mRealVM.mobile.value = phone
        mRealVM.checkedPolicy.set(intent.getBooleanExtra(EXTRA_CHECKED_POLICY, true))
        binding.tvCodeSubtitle.text =
            getString(R.string.login_new_code_subtitle, phone.toMaskedPhone())
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
            mRealVM.smsCode.value = code
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
    @SuppressLint("ClickableViewAccessibility")
    private fun initClicks() {
        binding.ivBack.setOnClickListener { finish() }
        binding.llCodeBoxes.setOnClickListener { showCodeKeyboard() }
        binding.etCode.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                showCodeKeyboard()
            }
            true
        }
        binding.tvResend.setOnClickListener {
            if (mRealVM.codeCounting.value == true) return@setOnClickListener
            resendSmsCode()
        }
    }

    /**
     * 重新获取验证码时展示获取中弹框，接口完成后关闭。
     */
    private fun resendSmsCode() {
        showLoginLoading(getString(R.string.login_new_getting_code))
        mRealVM.sendSmsCode(
            bizType = smsBizType(),
            onSuccess = {
                hideLoginLoading()
                binding.etCode.setText("")
            },
            onError = {
                it?.showToast()
                hideLoginLoading()
            }
        )
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
        showLoginLoading(getString(R.string.login_new_logging_in))
        when (businessType) {
            BUSINESS_TYPE_LOGIN -> {
                loginPhone()
            }

            BUSINESS_TYPE_BIND_PHONE -> {
                bindPhone()
                return
            }

            BUSINESS_TYPE_CHANGE_PHONE -> {
                changePhone()
                return
            }
        }
    }

    /**
     * 根据当前验证码业务类型返回短信验证码业务类型。
     */
    private fun smsBizType(): Int {
        return when (businessType) {
            BUSINESS_TYPE_LOGIN,
            BUSINESS_TYPE_BIND_PHONE -> SendSmsValidateCodeBody.BIZ_TYPE_LOGIN

            else -> SendSmsValidateCodeBody.BIZ_TYPE_CHANGE_PHONE
        }
    }

    /**
     * 手机号验证直接登录
     */
    private fun loginPhone() {
        mRealVM.phoneLogin(
            onLogin = {
                hideLoginLoading()
                if (isTaskRoot) {
                    // 避免IM账号被挤后（会关闭首页跳转登录页），再次登录无首页了
                    MainActivity.start(this)
                }
                finish()
            },
            onError = {
                hideLoginLoading()
                isLoggingIn = false
                binding.tvCodeError.visibility = View.VISIBLE
            }
        )
    }

    /**
     * 三方登录后绑定手机号，成功后保存三方登录返回的用户信息。
     */
    private fun bindPhone() {
        val userData = bindUserData
        if (userData == null) {
            hideLoginLoading()
            isLoggingIn = false
            binding.tvCodeError.visibility = View.VISIBLE
            return
        }
        mRealVM.bindPhone(
            userData = userData, onLogin = {
                hideLoginLoading()
                // 避免IM账号被挤后（会关闭首页跳转登录页），再次登录无首页了
                MainActivity.start(this)
                finish()
            },
            onError = {
                hideLoginLoading()
                isLoggingIn = false
                binding.tvCodeError.visibility = View.VISIBLE
            }
        )
    }

    /**
     * 修改手机号，成功后关闭验证码页。
     */
    private fun changePhone() {
        mRealVM.changePhone(
            onSuccess = {
                hideLoginLoading()
                finish()
            },
            onError = {
                hideLoginLoading()
                isLoggingIn = false
                binding.tvCodeError.visibility = View.VISIBLE
            }
        )
    }

    /**
     * 显示新版登录中弹窗。
     */
    private fun showLoginLoading(text: String? = null) {
        if (loginLoadingDialog?.isAdded == true) return
        loginLoadingDialog = LoginNewLoadingDialog.newInstance(text).apply {
            show(supportFragmentManager, LoginNewLoadingDialog.TAG)
        }
    }

    /**
     * 关闭新版登录中弹窗。
     */
    private fun hideLoginLoading() {
        loginLoadingDialog?.dismissAllowingStateLoss()
        loginLoadingDialog = null
    }

    /**
     * 聚焦隐藏输入框并弹出数字键盘。
     */
    private fun showCodeKeyboard() {
        KeyboardUtils.showKeyboard(binding.etCode)
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

    /**
     * 在手机号输入框底部展示接口返回的错误提示。
     */
    private fun showCodeError(message: String?) {
        binding.tvCodeError.text =
            message.takeUnless { it.isNullOrBlank() } ?: getString(R.string.login_new_code_error)
        binding.tvCodeError.visibility = View.VISIBLE
    }
}
