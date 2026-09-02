package com.shiyuan.fastclaw.feature.login.flash

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ActivityUtils
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.topActivity
import com.shiyuan.fastclaw.core.ext.onUserStatusChanged
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.AppEventParam
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.core.manager.FlashLoginManager
import com.shiyuan.fastclaw.core.manager.ShareManager
import com.shiyuan.fastclaw.databinding.DialogFlashLoginBinding
import com.shiyuan.fastclaw.feature.login.LoginActivity
import com.shiyuan.fastclaw.feature.login.PhoneNewPolicyDialog
import com.shiyuan.fastclaw.feature.login.phone.PhoneNewBindActivity
import com.shiyuan.fastclaw.feature.login.phone.PhoneLoginDialog
import com.shiyuan.fastclaw.feature.login.phone.PhoneLoginViewModel
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import com.shiyuan.fastclaw.feature.login.phone.PhoneNewLoginActivity
import com.umeng.socialize.bean.SHARE_MEDIA
import java.lang.ref.WeakReference

/**
 * 闪验登录入口
 * @author xiaoowuu
 */
class FlashLoginDialogFragment : BaseVMDialogFragment<DialogFlashLoginBinding, FlashLoginViewModel>(),
    FlashLoginManager.Listener, ShareManager.UmengLoginListener {

    private val shareManager by lazy { activity?.let { ShareManager(it) } }

    private var checkedPolicy = false
    private var loginSource: String? = null

    companion object {
        private var dialogRef: WeakReference<FlashLoginDialogFragment>? = null

        //是否选中了隐私政策
        const val CHECKED_POLICY = "checkedPolicy"
        private const val LOGIN_SOURCE = "loginSource"

        /**
         * 闪验登录
         */
        fun show(
            showToast: Boolean = false,
            checkedPolicy: Boolean = false,
            loginSource: String? = null
        ) {
            val manager = (topActivity as? FragmentActivity)?.supportFragmentManager ?: return
            // 外部可能连续触发登录拦截；已有闪验承载弹框时直接复用，避免重复拉起一键登录页
            dialogRef?.get()?.let { return }
            if (showToast) {
                showToast(R.string.toast_login_please)
            }
            val dialog = FlashLoginDialogFragment()
            dialog.arguments = Bundle().apply {
                putBoolean(CHECKED_POLICY, checkedPolicy)
                putString(LOGIN_SOURCE, loginSource)
            }
            dialog.show(manager)
            dialogRef = WeakReference(dialog)
        }


        private fun unRegister() {
            FlashLoginManager.removeListener()
            dialogRef?.clear()
            dialogRef = null
            FlashLoginManager.finishAuthActivity()
            ActivityUtils.finishActivity(LoginActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FlashLoginManager.setListener(this)
        checkedPolicy = arguments?.getBoolean(CHECKED_POLICY) ?: false
        loginSource = arguments?.getString(LOGIN_SOURCE)
    }

    override fun onStart() {
        super.onStart()
        isCancelable = false
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onDestroy() {
        FlashLoginManager.removeListener()
        unRegister()
        super.onDestroy()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { Dialog(it, R.style.LoadingDialog) } ?: super.onCreateDialog(
            savedInstanceState
        )
    }

    override fun onPrepareView() {
        super.onPrepareView()
        bindingNullable?.root?.visibility = View.GONE
        FlashLoginManager.login(
            onOpen = {
            },
            onOpenFail = { _, _ ->
                loginByPhoneDialog(true)
            },
            onLogin = { token ->
                mRealVM.loginByFlashMobile(token) {
                    shouldDismiss()
                }
            },
            onLoginFail = { _, _ ->
                showToast(R.string.toast_sign_in_fail)
                FlashLoginManager.hideLoading()
            },
            onCancel = {
                shouldDismiss()
            },
            onOtherLogin = { type ->
                //type 1:手机号 2:QQ 3:微信
                when (type) {
                    1 -> {
                        loginByPhoneDialog(true)
                    }

                    3 -> {
                        loginByThird(SHARE_MEDIA.WEIXIN)
                    }
                }
            }
        )
        observeEvents {
            onUserStatusChanged {
                dialogRef?.get()?.dismissAllowingStateLoss()
                unRegister()
            }
        }
    }

    private fun loginByThird(platform: SHARE_MEDIA) {
        EventManager.onAppTrack(
            AppEvent.KLX_CLICK_LOGIN_BTN, mutableMapOf(AppEventParam.KEY_LOGIN_TYPE to AppEventParam.VALUE_LOGIN_TYPE_WECHAT))
        if (FlashLoginManager.checkedPolicy) {
            shareManager?.login(platform, this)
        } else {
            showPolicyDialog {
                shareManager?.login(platform, this)
            }
        }
    }

    private fun shouldDismiss() {
        bindingNullable?.root?.visibility = View.GONE
        dismissAllowingStateLoss()
        unRegister()
    }

    /**
     * 手机号登录
     */
    private fun loginByPhoneDialog(dismiss: Boolean) {
        // 首次进入时 hasStartedPhoneLogin 为 false，会继续往下执行；置 true 后用于拦截重复回调
        EventManager.onAppTrack(AppEvent.KLX_VIEW_LOGIN, mutableMapOf(AppEventParam.KEY_LOGIN_TRIGGER_SOURCE to loginSource))
        FlashLoginManager.finishAuthActivity()
        PhoneNewLoginActivity.start(checkedPolicy = checkedPolicy)
        if (dismiss) {
            isCancelable = true
            shouldDismiss()
        }
    }

    /**
     * 跳转闪验授权页中的运营商协议或平台协议。
     */
    override fun toPolicy(url: String?, name: String?) {
        activity?.let { SimpleBrowserActivity.launch(it, url ?: "", name ?: "") }
    }

    /**
     * 闪验一键登录未勾选协议时，展示二次确认协议弹框。
     */
    override fun showPolicyDialog() {
        showPolicyDialog {
            FlashLoginManager.agreePolicyAndLogin()
        }
    }

    /**
     * 展示登录协议弹框，并在用户同意后继续执行当前登录动作。
     */
    private fun showPolicyDialog(onAgree: () -> Unit) {
        PhoneNewPolicyDialog.show(
            activity = topActivity,
            operatorName = FlashLoginManager.operatorPolicyName,
            operatorUrl = FlashLoginManager.operatorPolicyUrl
        ) { agree ->
            if (agree) {
                checkedPolicy = true
                FlashLoginManager.agreePolicy()
                onAgree()
            }
        }
    }

    override fun onStart(platform: SHARE_MEDIA?) {
        Logger.i("onStart platform:$platform")
    }

    override fun onAuthed(platform: SHARE_MEDIA?, data: MutableMap<String, String>?) {
        Logger.i("onAuthed platform:$platform data:$data")
        val loginType = when (platform) {
            SHARE_MEDIA.WEIXIN -> 3
            SHARE_MEDIA.QQ -> 4
            else -> {
                shouldDismiss()
                return
            }
        }
        mRealVM.loginByThird(loginType, data) { needBindMobile, userData ->
            if (needBindMobile && userData != null) {
                PhoneNewBindActivity.start(requireActivity(), userData)
            }
            shouldDismiss()
        }
    }

    override fun onError(e: Exception?, platform: SHARE_MEDIA?, onCancel: Boolean) {
        Logger.i("onError e:$e platform:$platform onCancel:$onCancel")
        shouldDismiss()
    }

}
