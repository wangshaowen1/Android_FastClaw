package com.kgmedia.fastfilm.feature.main

import android.view.Gravity
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ScreenUtils
import com.kgmedia.fastfilm.common.ext.getScreenWidth
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.DialogActivityBinding
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 活动弹窗
 */
class ActivityDialog : BaseDialogFragment<DialogActivityBinding>() {

    companion object {
        fun show() {
            val activity = topActivity as? FragmentActivity ?: return
            ActivityDialog().show(activity.supportFragmentManager)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = ScreenUtils.getScreenWidth()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.image.minimumHeight = getScreenWidth()
        binding.image.setOnSingleClickListener {
        }
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }
    }
}