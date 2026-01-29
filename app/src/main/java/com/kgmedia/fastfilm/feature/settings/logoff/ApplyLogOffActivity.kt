package com.kgmedia.fastfilm.feature.settings.logoff

import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.databinding.ActivityApplyLogOffBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast

/**
 * 注销账号-提交
 */
class ApplyLogOffActivity : BaseViewModelActivity<ActivityApplyLogOffBinding, ApplyLogOffViewModel>() {

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        if (view?.id == R.id.btnLogOff) {
            logOff()
        }
    }

    private fun logOff() {
        val content = mRealVM.content.value
        if (content.isNullOrEmpty() || content.length < 10) {
            "申请理由不得低于10个字".showToast()
            return
        }
        showConfirmPrompt(title = "是否确认提交申请", cancelText = "暂不", confirmText = "确定") {
            mRealVM.commitLogoff(content)
        }
    }

}