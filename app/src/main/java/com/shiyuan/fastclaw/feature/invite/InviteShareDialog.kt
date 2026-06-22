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
import com.blankj.utilcode.util.ClipboardUtils
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.toAlbum
import com.shiyuan.fastclaw.common.ext.toCache
import com.shiyuan.fastclaw.common.permissions.requestRead2WritePermissions
import com.shiyuan.fastclaw.core.manager.ShareManager
import com.shiyuan.fastclaw.databinding.DialogInviteShareBinding
import com.umeng.socialize.UMShareListener
import com.umeng.socialize.bean.SHARE_MEDIA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InviteShareDialog : BaseDialogFragment<DialogInviteShareBinding>() {

    companion object {
        fun show(manager: FragmentManager): InviteShareDialog {
            return InviteShareDialog().apply {
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
            sharePoster(SHARE_MEDIA.WEIXIN)
        }
        binding.llWechatMoments.setOnSingleClickListener {
            sharePoster(SHARE_MEDIA.WEIXIN_CIRCLE)
        }
        binding.llSaveImage.setOnSingleClickListener {
            savePosterToAlbum()
        }
        binding.llCopyLink.setOnSingleClickListener {
            copyInviteText()
        }
    }

    private fun sharePoster(platform: SHARE_MEDIA) {
        val activity = activity ?: return
        val posterFile = binding.sharePosterContainer.toCache()
        if (posterFile?.exists() != true) {
            showToast(R.string.toast_save_fail)
            return
        }
        ShareManager(activity).doShare(posterFile, platform, object : UMShareListener {
            override fun onStart(platform: SHARE_MEDIA?) = Unit

            override fun onResult(platform: SHARE_MEDIA?) = Unit

            override fun onError(platform: SHARE_MEDIA?, throwable: Throwable?) = Unit

            override fun onCancel(platform: SHARE_MEDIA?) = Unit
        })
        dismissAllowingStateLoss()
    }

    private fun savePosterToAlbum() {
        requestRead2WritePermissions { granted ->
            if (granted) {
                savePosterToAlbumWithPermission()
            }
        }
    }

    private fun savePosterToAlbumWithPermission() {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                binding.sharePosterContainer.toAlbum()
            }
            if (file?.exists() == true) {
                val manager = parentFragmentManager
                dismissAllowingStateLoss()
                InviteCopySuccessDialog.show(manager, R.string.invite_save_image_success)
                return@launch
            }
            showToast(R.string.toast_save_fail)
            dismissAllowingStateLoss()
        }
    }

    private fun copyInviteText() {
        ClipboardUtils.copyText(getString(R.string.invite_share_copy_text))
        val manager = parentFragmentManager
        dismissAllowingStateLoss()
        InviteCopySuccessDialog.show(manager)
    }
}
