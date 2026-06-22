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
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ScreenUtils
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogInviteCopySuccessBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InviteCopySuccessDialog : BaseDialogFragment<DialogInviteCopySuccessBinding>() {

    companion object {
        private const val DISMISS_DELAY_MILLIS = 1500L
        private const val DESIGN_WIDTH = 322f
        private const val DESIGN_SCREEN_WIDTH = 375f
        private const val TOP_OFFSET_DP = 95
        private const val ARG_MESSAGE_RES = "messageRes"

        fun show(
            manager: FragmentManager,
            messageRes: Int = R.string.invite_copy_success
        ): InviteCopySuccessDialog {
            return InviteCopySuccessDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_MESSAGE_RES, messageRes)
                }
                show(manager)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar).apply {
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                dialogWindow.statusBarColor = Color.TRANSPARENT
            }
            dialogWindow.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            val attr = dialogWindow.attributes
            attr.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            attr.y = TOP_OFFSET_DP.dp
            dialogWindow.attributes = attr
            dialogWindow.setLayout(
                (ScreenUtils.getScreenWidth() * DESIGN_WIDTH / DESIGN_SCREEN_WIDTH).toInt(),
                58.dp
            )
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvSuccessMessage.setText(
            arguments?.getInt(ARG_MESSAGE_RES) ?: R.string.invite_copy_success
        )
        binding.root.setOnClickListener {
            dismissAllowingStateLoss()
        }
        lifecycleScope.launch {
            delay(DISMISS_DELAY_MILLIS)
            dismissAllowingStateLoss()
        }
    }
}
