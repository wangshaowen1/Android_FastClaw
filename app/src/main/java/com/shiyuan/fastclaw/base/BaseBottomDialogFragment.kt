package com.shiyuan.fastclaw.base

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ViewDataBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.viewmodel.BaseLayoutViewModel

/**
 * @desc：底部弹框 - 适配底部导航栏
 * @author：CaoWei
 * @date：2026/7/6 12:47
 */
abstract class BaseBottomDialogFragment<V : ViewDataBinding, T : BaseLayoutViewModel>: BaseBottomPushVMDialogFragment<V, T>() {

    override fun onStart() {
        super.onStart()
        setupWindowStyle()

    }

    private fun setupWindowStyle() {
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

    private fun updateNavigationBarPaddingBottom() {
        val root = binding.root
        setRootPaddingBottom(root, getNavigationBarHeight())
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
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
        root.post {
            ViewCompat.requestApplyInsets(root)
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

    private fun setRootPaddingBottom(root: View, paddingBottom: Int) {
        root.setPadding(
            root.paddingLeft,
            root.paddingTop,
            root.paddingRight,
            paddingBottom
        )
    }
}
