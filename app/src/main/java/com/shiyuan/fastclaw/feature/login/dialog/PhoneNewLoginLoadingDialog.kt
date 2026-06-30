package com.shiyuan.fastclaw.feature.login.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import com.nhsd.basic.BaseDialogFragment
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogPhoneNewLoginLoadingBinding

class PhoneNewLoginLoadingDialog : BaseDialogFragment<DialogPhoneNewLoginLoadingBinding>() {

    companion object {
        const val TAG = "PhoneNewLoginLoadingDialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), R.style.LoadingDialog).apply {
            setCanceledOnTouchOutside(false)
        }
    }

    /**
     * 按登录中设计稿设置弹窗尺寸和位置。
     */
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = dp(139)
                attr.height = dp(126)
                attr.gravity = Gravity.CENTER
                attr.y = -dp(18)
                dialogWindow.attributes = attr
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }
}
