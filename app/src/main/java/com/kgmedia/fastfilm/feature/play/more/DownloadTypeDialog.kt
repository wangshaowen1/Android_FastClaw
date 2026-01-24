package com.kgmedia.fastfilm.feature.play.more

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.databinding.DialogDownloadBinding
import com.kgmedia.fastfilm.feature.payment.vip.MembershipBenefitsActivity
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import kotlin.math.min

/**
 * 下载方式弹窗
 */
class DownloadTypeDialog : BaseBottomPushVMDialogFragment<DialogDownloadBinding, DownloadTypeViewModel>() {

    companion object {

        fun show(launcher: DownloadTypeLauncher, cover: String?, title: String?, author: String?) {
            val fragmentManager = launcher.fragmentManager() ?: return
            val dialog = DownloadTypeDialog()
            dialog.arguments = Bundle().apply {
                putString(Constants.Extra.COVER, cover)
                putString(Constants.Extra.TITLE, title)
                putString(Constants.Extra.NAME, author)
            }
            dialog.show(fragmentManager)
        }
    }

    private val launcher by lazy { (parentFragment as? DownloadTypeLauncher) ?: (activity as? DownloadTypeLauncher) }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = min(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight())
                attr.height = WindowManager.LayoutParams.WRAP_CONTENT
                dialogWindow.attributes = attr
            }
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnDownload -> {
                launcher?.downloadVideo(true)
                dismissAllowingStateLoss()
            }

            R.id.btnDownloadNoWatermark -> {
                if (User.get().isVip.not()) {
                    MembershipBenefitsActivity.start(activity, MembershipBenefitsActivity.Scene.Download, true)
                    return
                }
                launcher?.downloadVideo(false)
                dismissAllowingStateLoss()
            }
        }
    }

    interface DownloadTypeLauncher {
        fun fragmentManager(): FragmentManager?
        fun downloadVideo(watermark: Boolean)
    }
}