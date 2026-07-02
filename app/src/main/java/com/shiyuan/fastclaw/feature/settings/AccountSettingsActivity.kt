package com.shiyuan.fastclaw.feature.settings

import android.app.Activity
import android.content.Intent
import android.view.View
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.showConfirmPrompt
import com.shiyuan.fastclaw.core.manager.ShareManager
import com.shiyuan.fastclaw.databinding.ActivityAccountSettingsBinding
import com.shiyuan.fastclaw.feature.login.phone.PhoneNewChangeActivity
import com.shiyuan.fastclaw.feature.settings.logoff.LogoffNewActivity
import com.umeng.socialize.UMShareAPI
import com.umeng.socialize.bean.SHARE_MEDIA

/**
 *  账号设置页
 *  @author：CaoWei
 *  @date：2026/7/1 15:49
 */
class AccountSettingsActivity : BaseViewModelActivity<ActivityAccountSettingsBinding, SettingsViewModel>(),
    ShareManager.UmengLoginListener {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, AccountSettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }

    private val shareManager by lazy { ShareManager(this) }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

    /** 页面重新可见时刷新手机号脱敏展示。 */
    override fun onResume() {
        super.onResume()
        mRealVM.refreshUserInfo()
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.rlPhone ->
                PhoneNewChangeActivity.start(this)
            R.id.rlWechat ->
                handleWechatClick()
            R.id.tvLogoffAccount ->
                LogoffNewActivity.start(this)
        }
    }

    /** 根据当前微信绑定状态处理点击事件。 */
    private fun handleWechatClick() {
        if (mRealVM.isWechatBound.get()) {
            showUnbindDialog()
        } else {
            bindWechat()
        }
    }

    /** 拉起微信授权，未安装微信时按设计提示。 */
    private fun bindWechat() {
        if (!UMShareAPI.get(this).isInstall(this, SHARE_MEDIA.WEIXIN)) {
            showToast(R.string.wechat_client_not_found)
            return
        }
        shareManager.login(SHARE_MEDIA.WEIXIN, this)
    }

    /** 展示微信解绑确认弹窗。 */
    private fun showUnbindDialog() {
        showConfirmPrompt(
            tip = getString(R.string.unbind_wechat_confirm_content),
            title = getString(R.string.unbind_wechat_confirm_title),
            cancelText = getString(R.string.cancel),
            confirmText = getString(R.string.confirm),
            confirm = {
                mRealVM.unbindWechat()
            }
        )
    }

    /** 微信授权开始。 */
    override fun onStart(platform: SHARE_MEDIA?) {
    }

    /** 微信授权成功后刷新页面展示状态。 */
    override fun onAuthed(platform: SHARE_MEDIA?, data: MutableMap<String, String>?) {
        if (platform != SHARE_MEDIA.WEIXIN) return
        mRealVM.updateWechatBound(data?.get("name"))
        mRealVM.refreshUserInfo()
        showToast(R.string.bind_success)
    }

    /** 微信授权失败或取消。 */
    override fun onError(e: Exception?, platform: SHARE_MEDIA?, onCancel: Boolean) {
    }
}
