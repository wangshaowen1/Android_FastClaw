package com.shiyuan.fastclaw.feature.invite

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentManager
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.DialogInviteShareBinding

class InviteShareDialog : BaseDialogFragment<DialogInviteShareBinding>() {

    companion object {
        fun show(manager: FragmentManager): InviteShareDialog {
            return InviteShareDialog().apply {
                show(manager)
            }
        }
    }

    var onShareActionClick: ((InviteShareAction) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar).apply {
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                dialogWindow.statusBarColor = Color.TRANSPARENT
            }
            dialogWindow.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            dialogWindow.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            val attr = dialogWindow.attributes
            attr.gravity = Gravity.BOTTOM
            dialogWindow.attributes = attr
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.sharePanel.setOnClickListener {
            // Consume panel clicks so tapping inside the sheet does not close it.
        }
        binding.tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.llWechatFriend.setOnSingleClickListener {
            dispatchAction(InviteShareAction.WECHAT_FRIEND)
        }
        binding.llWechatMoments.setOnSingleClickListener {
            dispatchAction(InviteShareAction.WECHAT_MOMENTS)
        }
        binding.llSaveImage.setOnSingleClickListener {
            dispatchAction(InviteShareAction.SAVE_IMAGE)
        }
        binding.llCopyLink.setOnSingleClickListener {
            dispatchAction(InviteShareAction.COPY_LINK)
        }
    }

    private fun dispatchAction(action: InviteShareAction) {
        dismissAllowingStateLoss()
        onShareActionClick?.invoke(action)
    }
}

enum class InviteShareAction {
    WECHAT_FRIEND,
    WECHAT_MOMENTS,
    SAVE_IMAGE,
    COPY_LINK
}
