package com.shiyuan.fastclaw.feature.settings

import android.app.Activity
import android.content.Intent
import android.view.View
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.showConfirmPrompt
import com.shiyuan.fastclaw.databinding.ActivityLogoffNewBinding
import com.shiyuan.fastclaw.feature.settings.vm.LogoffViewModel

/**
 * @desc：新版注销页面
 * @author：CaoWei
 * @date：2026/7/1 16:45
 */
class LogoffNewActivity : BaseViewModelActivity<ActivityLogoffNewBinding, LogoffViewModel>() {

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, LogoffNewActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        if (view?.id == R.id.btnConfirmLogoff) {
            showConfirmLogoffDialog()
        }
    }

    /** 展示确认注销弹窗。 */
    private fun showConfirmLogoffDialog() {
        showConfirmPrompt(
            tip = getString(R.string.logoff_new_confirm_content),
            title = getString(R.string.logoff_new_confirm_title),
            cancelText = getString(R.string.cancel),
            confirmText = getString(R.string.unregister_confirm)
        ) {
            mRealVM.commitLogoff()
        }
    }
}