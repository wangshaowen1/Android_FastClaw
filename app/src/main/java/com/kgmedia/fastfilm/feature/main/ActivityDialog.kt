package com.kgmedia.fastfilm.feature.main

import android.view.Gravity
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ScreenUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.getScreenWidth
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.databinding.loadUri
import com.kgmedia.fastfilm.core.network.apis.response.PopupInfo
import com.kgmedia.fastfilm.databinding.DialogActivityBinding
import com.kgmedia.fastfilm.router.AppRouter
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 活动弹窗
 */
class ActivityDialog : BaseDialogFragment<DialogActivityBinding>() {

    companion object {
        fun show(popup: PopupInfo?) {
            val activity = topActivity as? FragmentActivity ?: return
            ActivityDialog().apply {
                arguments?.putParcelable(Constants.Extra.DATA, popup)
            }.show(activity.supportFragmentManager)
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
        val popup = arguments?.getParcelableValue(Constants.Extra.DATA, PopupInfo::class.java)
        binding.image.minimumHeight = getScreenWidth()
        loadUri(binding.image, popup?.imageUrl, R.drawable.img_default_cover_1_1.getResDrawable())
        binding.image.setOnSingleClickListener {
            AppRouter.jump(popup?.jumpUri)
            dismissAllowingStateLoss()
        }
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }
    }
}