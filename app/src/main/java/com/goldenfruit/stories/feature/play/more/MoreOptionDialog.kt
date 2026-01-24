package com.kgmedia.fastfilm.feature.play.more

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.databinding.DialogShortMoreBinding
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorActivity
import com.kgmedia.fastfilm.feature.play.ShortDramaPlayerViewModel
import com.kgmedia.fastfilm.feature.play.more.DownloadTypeDialog.DownloadTypeLauncher
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 更多弹框
 */
class MoreOptionDialog : BaseBottomPushVMDialogFragment<DialogShortMoreBinding, ShortDramaPlayerViewModel>(useSharedViewModel = true), DownloadTypeDialog.DownloadTypeLauncher {

    companion object {
        fun show(
            launcher: DownloadTypeLauncher,
            dramaId: String?,
            dramaName: String?,
            dramaIntro: String?
        ) {
            val manager = launcher.fragmentManager() ?: return
            val dialog = MoreOptionDialog()
            dialog.arguments = bundleOf(
                Constants.Extra.DRAMA_ID to dramaId,
                Constants.Extra.DRAMA_NAME to dramaName,
                Constants.Extra.DRAMA_INTRO to dramaIntro
            )
            dialog.show(manager)
        }
    }

    private val launcher by lazy { (parentFragment as? DownloadTypeLauncher) ?: (activity as? DownloadTypeLauncher) }
    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.tv_edit_info -> {
                ShortInfoEditorDialog.show(
                    parentFragmentManager,
                    arguments?.getString(Constants.Extra.DRAMA_ID),
                    arguments?.getString(Constants.Extra.DRAMA_NAME),
                    arguments?.getString(Constants.Extra.DRAMA_INTRO)
                )
            }

            R.id.tv_download -> {
                DownloadTypeDialog.show(this, mRealVM.shortCover.get(), mRealVM.shortName.get(), mRealVM.dramaDetail.value?.artist?.nickName)
            }

            R.id.tv_reedit -> {
                ShortDramaEditorActivity.start(mRealVM.dramaId)
            }

            R.id.tv_delete -> {
                mActivity?.showConfirmPrompt(tip = "删除后视频不可恢复，请谨慎操作！", title = "确认删除视频吗？") {
                    mRealVM.deleteShot()
                }
            }
        }
    }

    override fun fragmentManager(): FragmentManager? = childFragmentManager

    override fun downloadVideo(watermark: Boolean) {
        launcher?.downloadVideo(watermark)
        dismissAllowingStateLoss()
    }
}