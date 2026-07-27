package com.shiyuan.fastclaw.feature.clawagent.dialog

import android.view.Gravity
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.nhsd.basic.BaseDialogFragment
import com.shiyuan.fastclaw.databinding.DialogNotificationsSwitchBinding

/** 消息通知开关弹窗
 * @createtime 2026年05月20日 14:25
 * @author TooWhiteT
 */

class NotificationsSwitchDialog : BaseDialogFragment<DialogNotificationsSwitchBinding>() {

    companion object {
        fun show(manager: FragmentManager): NotificationsSwitchDialog {
            val dialog = NotificationsSwitchDialog()
            dialog.show(manager)
            return dialog
        }
    }

    var clickListener: ((flag: Boolean) -> Unit)? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = (ScreenUtils.getScreenWidth() * 296f / 375f).toInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvAgree.setOnClickListener {
            dismiss()
            clickListener?.invoke(true)
        }
        binding.tvCancle.setOnClickListener {
            dismiss()
            clickListener?.invoke(false)
        }
    }
}