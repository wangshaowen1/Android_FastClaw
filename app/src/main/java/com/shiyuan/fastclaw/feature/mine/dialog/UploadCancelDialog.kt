package com.shiyuan.fastclaw.feature.mine.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.DialogUploadCancelBinding
import com.shiyuan.fastclaw.feature.report.ReportViewModel

/**
 * 上传取消dialog
 *
 */
class UploadCancelDialog : BaseBottomPushVMDialogFragment<DialogUploadCancelBinding, ReportViewModel>() {

    companion object {
        const val TAG = "UploadCancelDialog"
        var uploadListener: (() -> Unit)? = null

        fun show(manager: FragmentManager, uploadListener: (() -> Unit)? = null): UploadCancelDialog {
            val dialog = UploadCancelDialog()
            this.uploadListener = uploadListener
            dialog.show(manager)
            return dialog
        }
    }


    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                dialogWindow.navigationBarColor = Color.TRANSPARENT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                dialogWindow.isNavigationBarContrastEnforced = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                dialogWindow.setDecorFitsSystemWindows(false)
            }

            var systemUiVisibility = dialogWindow.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            dialogWindow.decorView.systemUiVisibility = systemUiVisibility
        }
        updateNavigationBarPaddingBottom()
    }

    private fun initView() {
        binding.tvCancel.setOnSingleClickListener { dismiss() }
        binding.tvUpload.setOnSingleClickListener {
            uploadListener?.invoke()
            dismiss()
        }
    }

    private fun updateNavigationBarPaddingBottom() {
        setRootPaddingBottom(getNavigationBarHeight())
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            if (navigationBarHeight > 0) {
                view.setPadding(
                    view.paddingLeft,
                    view.paddingTop,
                    view.paddingRight,
                    navigationBarHeight
                )
            }
            insets
        }
        binding.root.post {
            ViewCompat.requestApplyInsets(binding.root)
        }
    }

    private fun getNavigationBarHeight(): Int {
        val activityInsets = ViewCompat.getRootWindowInsets(requireActivity().window.decorView)
        val activityNavigationBarHeight =
            activityInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        if (activityNavigationBarHeight > 0) {
            return activityNavigationBarHeight
        }

        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun setRootPaddingBottom(paddingBottom: Int) {
        binding.root.setPadding(
            binding.root.paddingLeft,
            binding.root.paddingTop,
            binding.root.paddingRight,
            paddingBottom
        )
    }

}
