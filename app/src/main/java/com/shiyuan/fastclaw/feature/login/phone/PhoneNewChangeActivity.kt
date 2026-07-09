package com.shiyuan.fastclaw.feature.login.phone

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.isRealMobile
import com.shiyuan.fastclaw.common.ext.showConfirmPrompt
import com.shiyuan.fastclaw.common.utils.KeyboardUtils
import com.shiyuan.fastclaw.core.network.apis.request.CheckMobileBindStatusBody.Companion.SCENE_CHANGE_MOBILE
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
        showKeyboard()
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
     * 发送换绑手机号验证码，成功后进入验证码页的换绑模式。
     */
    private fun requestCode() {
        if (!updatePhoneError(showOnlyWhenComplete = true)) return
        val phone = binding.etPhone.text?.toString().orEmpty()
        mRealVM.mobile.value = phone
        mRealVM.checkMobileBindStatus(scene = SCENE_CHANGE_MOBILE, bindTicket = null, onSuccess = { data ->
                if (data?.isSuccess() == true) {
                    sendChangeMobileCode(phone)
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
     * 手机号前置校验通过后发送换绑验证码。
     */
    private fun sendChangeMobileCode(phone: String) {
        mRealVM.sendSmsCode(
            startCountdownOnSuccess = false,
            bizType = SendSmsValidateCodeBody.BIZ_TYPE_CHANGE_PHONE,
            onSuccess = {
                PhoneNewLoginCodeActivity.startForChangePhone(this, phone)
                finish()
            },
            onError = {
                showPhoneError(it)
            }
        )
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
        val valid = phone.isRealMobile()
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
