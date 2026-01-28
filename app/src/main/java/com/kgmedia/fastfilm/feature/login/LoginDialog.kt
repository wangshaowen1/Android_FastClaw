package com.kgmedia.fastfilm.feature.login

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.manager.AppEvent
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.ShareManager
import com.kgmedia.fastfilm.core.network.apis.request.LoginByThirdBody
import com.kgmedia.fastfilm.databinding.DialogLoginBinding
import com.kgmedia.fastfilm.feature.login.flash.FlashLoginDialogFragment
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import com.umeng.socialize.bean.SHARE_MEDIA

class LoginDialog : BaseViewModelActivity<DialogLoginBinding, LoginViewModel>(),
    ShareManager.UmengLoginListener {

    companion object {
        var checkedPolicy = false
        fun show(showToast: Boolean = false) {
            if (showToast) {
                "请先登录".showToast()
            }
            val activity = topActivity ?: return
            activity.startActivity(Intent(activity, LoginDialog::class.java))
            activity.overridePendingTransition(
                R.anim.push_bottom_in,
                R.anim.push_bottom_out
            )
        }
    }


    private val shareManager by lazy { ShareManager(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventManager.onAppTrack(AppEvent.JG_VIEW_LOGIN_PAGE)
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnClose -> {
                //点x跳写歌界面的问题  看MineFragment-> mRealVM.noLogIn.observe这里的处理逻辑
                finishOnly()
            }

            R.id.btnWechat -> {
                if (mRealVM.checkedPolicy.get()) {
                    shareManager.login(SHARE_MEDIA.WEIXIN, this)
                } else {
                    binding.toast.showToast(R.string.please_check_policy_below.getResString())
                }
            }

            R.id.btnPhone -> {
                checkedPolicy = mRealVM.checkedPolicy.get()
                FlashLoginDialogFragment.show(false, checkedPolicy = mRealVM.checkedPolicy.get())
            }
        }
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
                finish()
                return
            }
        }
        mRealVM.loginByThird(loginType, data) {
            finishOnly()
        }
    }

    override fun onError(e: Exception?, platform: SHARE_MEDIA?, onCancel: Boolean) {
        Logger.d("onError e:$e platform:$platform onCancel:$onCancel")
        finishOnly()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(false)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .transparentNavigationBar()
            .navigationBarDarkIcon(false)
            .init()
    }

    private var enterAnim = 0
    private var exitAnim = 0


    /**
     * finish动画处理
     */
    private fun finishOnly() {
        enterAnim = 0
        exitAnim = R.anim.push_bottom_out
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(enterAnim, exitAnim)
    }
}