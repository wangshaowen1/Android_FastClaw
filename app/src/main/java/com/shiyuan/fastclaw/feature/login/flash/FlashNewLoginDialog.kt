package com.shiyuan.fastclaw.feature.login.flash

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ActivityUtils
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.manager.ShareManager
import com.shiyuan.fastclaw.core.network.apis.request.LoginByThirdBody
import com.shiyuan.fastclaw.databinding.DialogFlashNewLoginBinding
import com.shiyuan.fastclaw.feature.login.LoginActivity
import com.shiyuan.fastclaw.feature.login.dialog.PhoneNewPolicyDialog
import com.shiyuan.fastclaw.feature.login.phone.PhoneNewBindActivity
import com.shiyuan.fastclaw.feature.login.phone.PhoneNewLoginActivity
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.umeng.socialize.UMShareAPI
import com.umeng.socialize.bean.SHARE_MEDIA

/**
 * 新版一键登录底部弹框
 *  @author：CaoWei
 *  @date：2026/7/1 09:50
 */
class FlashNewLoginDialog : BaseBottomPushVMDialogFragment<DialogFlashNewLoginBinding, FlashLoginViewModel>(),
    ShareManager.UmengLoginListener {

    companion object {
        private const val CHECKED_POLICY = "checkedPolicy"

        /**
         * 展示新版一键登录底部弹框。
         */
        fun show(
            manager: FragmentManager,
            showToast: Boolean = false,
            checkedPolicy: Boolean = false,
        ): FlashNewLoginDialog {
            if (showToast) {
                showToast(R.string.toast_login_please)
            }
            return FlashNewLoginDialog().apply {
                arguments = android.os.Bundle().apply {
                    putBoolean(CHECKED_POLICY, checkedPolicy)
                }
                show(manager)
            }
        }
    }

    private val shareManager by lazy { ShareManager(requireActivity()) }

    /**
     * 初始化预取号、协议文案和登录结果监听。
     */
    override fun onPrepareView() {
        super.onPrepareView()
        mRealVM.initFlashLogin(arguments?.getBoolean(CHECKED_POLICY) ?: LoginActivity.checkedPolicy)
        initAgreement()
        observeVm()
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnClose -> finishOnly()
            R.id.btnOneKeyLogin -> loginByFlash()
            R.id.btnOtherPhone -> openPhoneLogin()
            R.id.layoutAgreement -> {
                mRealVM.checkedPolicy.set(!mRealVM.checkedPolicy.get())
            }

            R.id.btnWechat -> loginByWechat()
        }
    }

    /**
     * 监听预取号失败和一键登录成功事件。
     */
    private fun observeVm() {
        mRealVM.flashPreFetchFailed.observe(this) {
            if (!it) return@observe
            mRealVM.flashPreFetchFailed.value = false
            openPhoneLogin()
        }
        mRealVM.flashLoginResult.observe(this) {
            if (!it) return@observe
            mRealVM.flashLoginResult.value = false
            finishOnly()
            ActivityUtils.finishActivity(LoginActivity::class.java)
        }
    }

    /**
     * 本机号码一键登录，未勾选协议时先弹出协议确认。
     */
    private fun loginByFlash() {
        if (mRealVM.checkedPolicy.get()) {
            mRealVM.flashLoginVerification()
            return
        }
        val dialog = PhoneNewPolicyDialog.show(parentFragmentManager)
        dialog.callback = callback@{ agree ->
            if (!agree) return@callback
            mRealVM.checkedPolicy.set(true)
            mRealVM.flashLoginVerification()
        }
    }

    /**
     * 微信登录，未勾选协议时先弹出协议确认。
     */
    private fun loginByWechat() {
        if (!isWechatInstalled()) return
        if (mRealVM.checkedPolicy.get()) {
            startWechatLogin()
            return
        }
        val dialog = PhoneNewPolicyDialog.show(parentFragmentManager)
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
        val installed = UMShareAPI.get(requireActivity()).isInstall(requireActivity(), SHARE_MEDIA.WEIXIN)
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
     * 打开新版手机号登录页。
     */
    private fun openPhoneLogin() {
        val checkedPolicy = mRealVM.checkedPolicy.get()
        finishOnly()
        PhoneNewLoginActivity.start(false, checkedPolicy)
        ActivityUtils.finishActivity(LoginActivity::class.java)
    }

    /**
     * 构建底部协议富文本并设置协议点击跳转。
     */
    private fun initAgreement() {
        binding.tvAgreement.movementMethod = LinkMovementMethod.getInstance()
        binding.tvAgreement.highlightColor = Color.TRANSPARENT
        mRealVM.flashPhoneInfo.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.tvAgreement.text = buildAgreementText()
                }
            }
        )
        binding.tvAgreement.text = buildAgreementText()
    }

    /**
     * 生成协议文案，并给服务协议、隐私政策、账号须知和运营商协议加点击事件。
     */
    private fun buildAgreementText(): SpannableString {
        val operator = mRealVM.flashPhoneInfo.get()?.protocolName
            ?: mRealVM.flashPhoneInfo.get()?.authenticationService()
        val content = if (operator.isNullOrBlank()) {
            getString(R.string.flash_new_agreement_content_no_operator)
        } else {
            getString(R.string.flash_new_agreement_content, operator)
        }
        return SpannableString(content).apply {
            addLink(this, getString(R.string.flash_new_service_agreement), Config.get().serviceUrl)
            addLink(this, getString(R.string.flash_new_privacy_policy), Config.get().privacyUrl)
            addLink(this, getString(R.string.flash_new_account_terms), Config.get().serviceUrl)
            if (!operator.isNullOrBlank()) {
                addLink(this, operator, mRealVM.flashPhoneInfo.get()?.protocolUrl)
            }
        }
    }

    /**
     * 给协议名称增加点击跳转。
     */
    private fun addLink(text: SpannableString, target: String, url: String?) {
        val start = text.indexOf(target)
        if (start < 0) return
        text.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    SimpleBrowserActivity.launch(activity, url, target)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(requireContext(), R.color.color_2f6bff)
                    ds.isUnderlineText = false
                }
            },
            start,
            start + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    override fun onStart(platform: SHARE_MEDIA?) {
        Logger.d("onStart platform:$platform")
    }

    override fun onAuthed(platform: SHARE_MEDIA?, data: MutableMap<String, String>?) {
        Logger.d("onAuthed platform:$platform data:$data")
        val loginType = when (platform) {
            SHARE_MEDIA.WEIXIN -> LoginByThirdBody.LOGIN_TYPE_WECHAT
            SHARE_MEDIA.QQ -> LoginByThirdBody.LOGIN_TYPE_QQ
            else -> {
                finishOnly()
                return
            }
        }
        mRealVM.loginByThird(loginType, data) { needBindMobile, userData ->
            if (needBindMobile && userData != null) {
                PhoneNewBindActivity.start(requireActivity(), userData)
            }
            finishOnly()
            ActivityUtils.finishActivity(LoginActivity::class.java)
        }
    }

    override fun onError(e: Exception?, platform: SHARE_MEDIA?, onCancel: Boolean) {
        Logger.d("onError e:$e platform:$platform onCancel:$onCancel")
        finishOnly()
    }

    /**
     * 关闭底部弹框。
     */
    private fun finishOnly() {
        dismissAllowingStateLoss()
    }
}
