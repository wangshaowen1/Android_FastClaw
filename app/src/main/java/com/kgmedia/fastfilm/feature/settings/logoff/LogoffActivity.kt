package com.kgmedia.fastfilm.feature.settings.logoff

import android.app.Activity
import android.content.Intent
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.databinding.ActivityCloseAccountBinding
import com.nhsd.basic.BaseViewModelActivity

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
            activity?:return
            val intent = Intent(activity, LogoffActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        if (view?.id == R.id.btnCancel) {
            cancelLogoff()
        }
    }

    /**
     * 取消注销弹窗
     */
    private fun cancelLogoff() {
        showConfirmPrompt (tip = "是否确认取消注销申请", cancelText = "暂不"){
                mRealVM.cancelLogoff()
        }
    }
}