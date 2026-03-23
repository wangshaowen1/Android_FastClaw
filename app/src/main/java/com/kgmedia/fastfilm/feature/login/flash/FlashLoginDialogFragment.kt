package com.kgmedia.fastfilm.feature.login.flash

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ActivityUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.ext.onUserStatusChanged
import com.kgmedia.fastfilm.core.manager.FlashLoginManager
import com.kgmedia.fastfilm.core.manager.ShareManager
import com.kgmedia.fastfilm.databinding.DialogFlashLoginBinding
import com.kgmedia.fastfilm.feature.login.LoginActivity
import com.kgmedia.fastfilm.feature.login.phone.PhoneLoginDialog
import com.kgmedia.fastfilm.feature.login.phone.PhoneLoginViewModel
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import com.umeng.socialize.bean.SHARE_MEDIA
import java.lang.ref.WeakReference

/**
 * 闪验登录入口
 * @author xiaoowuu
 */
class FlashLoginDialogFragment :
    BaseVMDialogFragment<DialogFlashLoginBinding, FlashLoginViewModel>(),
    FlashLoginManager.Listener, ShareManager.UmengLoginListener {

    companion object {
        private var dialogRef: WeakReference<FlashLoginDialogFragment>? = null

        //是否选中了隐私政策
        const val CHECKED_POLICY = "checkedPolicy"

        /**
         * 闪验登录
         */
        fun show(
            manager: FragmentManager?,
            showToast: Boolean = false,
            checkedPolicy: Boolean = false
        ) {
            val manager =
                manager ?: (topActivity as? FragmentActivity)?.supportFragmentManager ?: return
            if (showToast) {
                showToast(R.string.toast_login_please)
            }
            val dialog = FlashLoginDialogFragment()
            dialog.arguments = Bundle().apply {
                putBoolean(CHECKED_POLICY, checkedPolicy)
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

    private val shareManager by lazy { activity?.let { ShareManager(it) } }

    private var checkedPolicy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FlashLoginManager.setListener(this)
        checkedPolicy = arguments?.getBoolean(CHECKED_POLICY) ?: false
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
        bindingNullable?.root?.visibility = View.VISIBLE
        FlashLoginManager.login(
            onOpen = {
            },
            onOpenFail = { _, _ ->
                loginByPhoneDialog(true)
                shouldDismiss()
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
        if (FlashLoginManager.checkedPolicy) {
            shareManager?.login(platform, this)
        } else {
            R.string.please_check_policy_below.getResString().showToast()
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
        LoginActivity.checkedPolicy = FlashLoginManager.checkedPolicy
        PhoneLoginDialog.show(
            showToast = false,
            checkedPolicy = FlashLoginManager.checkedPolicy,
            manager = this.parentFragmentManager,
            loginType = PhoneLoginViewModel.SHOW_VERIFY_CODE_LOGIN
        )
        //PhoneLoginDialogActivity.start(requireActivity(), checkedPolicy = checkedPolicy)
        if (dismiss) {
            isCancelable = true
            shouldDismiss()
        }
    }


    override fun toPolicy(url: String?, name: String?) {
        activity?.let { SimpleBrowserActivity.launch(it, url ?: "", name ?: "") }
    }

    override fun showPolicyDialog() {
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
        mRealVM.loginByThird(loginType, data) {
            shouldDismiss()
        }
    }

    override fun onError(e: Exception?, platform: SHARE_MEDIA?, onCancel: Boolean) {
        Logger.i("onError e:$e platform:$platform onCancel:$onCancel")
        shouldDismiss()
    }

}