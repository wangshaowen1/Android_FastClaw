package com.kgmedia.fastfilm.feature.settings.logoff

import android.app.Activity
import android.content.Intent
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.databinding.ActivityCloseAccountBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: 小元音乐圈
 * @Author: SZYMN
 * @Introduce: 注销账号页面
 * @Data: 2025/4/22  13:54
 * @Version: 1.0
 * *****************************************
 */
class LogoffActivity :
    BaseViewModelActivity<ActivityCloseAccountBinding, LogoffViewModel>() {
    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, LogoffActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        if (view?.id == R.id.btnCancel) {
            when (mRealVM.removeAccountState.get()) {
                LogoffViewModel.REMOVE_ACCOUNT_STATE_NOT_APPLY -> {}
                LogoffViewModel.REMOVE_ACCOUNT_STATE_APPLY -> {
                    if (mRealVM.checkedPolicy.get()==false){
                        "如需确认注销，请先阅读并同意注销说明".showToast()
                        return
                    }
                    showConfirmPrompt(
                        title = "是否注销账号",
                        cancelText = "取消",
                        confirmText = "确定"
                    ) {
                        mRealVM.commitLogoff("")
                    }
                }

                LogoffViewModel.REMOVE_ACCOUNT_STATE_APPLY_NOT_CLICK -> {
                    cancelLogoff()
                }
            }
        }
    }

    /**
     * 取消注销弹窗
     */
    private fun cancelLogoff() {
        showConfirmPrompt(tip = "是否确认取消注销申请", cancelText = "暂不") {
            mRealVM.cancelLogoff()
        }
    }
}