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
import com.shiyuan.fastclaw.core.network.apis.request.SendSmsValidateCodeBody
import com.shiyuan.fastclaw.databinding.ActivityPhoneNewChangeBinding

/**
 * 修改手机号页。
 *  @author：CaoWei
 *  @date：2026/7/2
 */
class PhoneNewChangeActivity : BaseViewModelActivity<ActivityPhoneNewChangeBinding, PhoneNewLoginViewModel>() {

    companion object {
        /**
         * 打开修改手机号页。
         */
        fun start(activity: Activity) {
            activity.startActivity(Intent(activity, PhoneNewChangeActivity::class.java))
        }
    }

    /**
     * 设置修改手机号页的沉浸式状态栏样式。
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
    }

    /**
     * 监听手机号输入并刷新错误提示、按钮状态和已有手机号提示。
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
        binding.etPhone.typeface = if (binding.etPhone.text.isNullOrEmpty()) {
            Typeface.DEFAULT
        } else {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }

    /**
     * 绑定返回和获取验证码点击事件。
     */
    private fun initClicks() {
        binding.ivBack.setOnClickListener { finish() }
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
     * 发送换绑手机号验证码，成功后进入验证码页的换绑模式。
     */
    private fun requestCode() {
        if (!updatePhoneError(showOnlyWhenComplete = true)) return
        val phone = binding.etPhone.text?.toString().orEmpty()
        mRealVM.mobile.value = phone
        mRealVM.sendSmsCode(
            startCountdownOnSuccess = false,
            bizType = SendSmsValidateCodeBody.BIZ_TYPE_CHANGE_PHONE,
            onError = { message ->
                showExistingPhoneToastIfNeeded(phone, message)
            }
        ) {
            PhoneNewCodeLoginActivity.startForChangePhone(this, phone)
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
        binding.tvPhoneError.visibility = if (showError) View.VISIBLE else View.INVISIBLE
        return valid
    }

    /**
     * 后端提示新手机号已存在时，按设计展示黑色提示卡。
     */
    private fun showExistingPhoneToastIfNeeded(phone: String, message: String?) {
        val shouldShow = message?.contains("存在") == true || message?.contains("注册") == true
        if (shouldShow) {
            getString(R.string.change_mobile_existing_tip, phone.toMaskedPhone()).showToast()
        } else {
            message?.showToast()
        }
    }

    /**
     * 脱敏展示手机号。
     */
    private fun String.toMaskedPhone(): String {
        if (length < 7) return this
        return replaceRange(3, 7, "****")
    }
}
