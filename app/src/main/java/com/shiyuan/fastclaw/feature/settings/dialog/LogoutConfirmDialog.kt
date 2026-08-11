package com.shiyuan.fastclaw.feature.settings.dialog

import android.view.Gravity
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogLogoutConfirmBinding

/**
 * 退出登录二次确认弹窗
 * 基础对话弹窗风格可基于此修改
 * @createtime 2026年05月18日 16:22
 * @author TooWhiteT
 */

class LogoutConfirmDialog : BaseDialogFragment<DialogLogoutConfirmBinding>() {

    companion object {
        fun show(manager: FragmentManager): LogoutConfirmDialog {
            val dialog = LogoutConfirmDialog()
            dialog.show(manager)
            return dialog
        }
    }

    var clickListener: ((flag: Boolean) -> Unit)? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                // w/sw = 296/375
                // w = sw * 296 / 375
                attr.width = (ScreenUtils.getScreenWidth() * 296f / 375f).toInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()

        binding.tvTitle.text = R.string.logout_dialog_title.getResString()
        binding.tvContent.text = R.string.logout_dialog_content.getResString()
        binding.tvReject.text = R.string.logout_dialog_cancel.getResString()
        binding.tvAgree.text = R.string.logout_dialog_confirm.getResString()

        binding.tvReject.setOnClickListener {
            dismiss()
            clickListener?.invoke(false)
        }
        binding.tvAgree.setOnClickListener {
            dismiss()
            clickListener?.invoke(true)
        }
    }
}