package com.kgmedia.fastfilm.feature.series.share

import android.graphics.Bitmap
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ClipboardUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.getImageBitmap
import com.kgmedia.fastfilm.core.manager.ShareManager
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.databinding.DialogEpisodeShareBinding
import com.kgmedia.fastfilm.feature.play.more.DownloadTypeDialog
import com.kgmedia.fastfilm.feature.play.more.ShortInfoEditorDialog
import com.kgmedia.fastfilm.feature.settings.report.ReportActivity
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast
import com.nhsd.share.KsShareException
import com.umeng.socialize.UMShareListener
import com.umeng.socialize.bean.SHARE_MEDIA

/**
 * 单集-分享
 */
class EpisodeShareDialog : BaseBottomPushVMDialogFragment<DialogEpisodeShareBinding, EpisodeShareViewModel>(), UMShareListener, DownloadTypeDialog.DownloadTypeLauncher {
    companion object {

        /**
         * 显示分享弹窗
         */
        fun show(launcher: ShareLauncher, drama: ShortDramaInfo) {
            val fragmentManager = launcher.fragmentManager() ?: return
            EpisodeShareDialog().apply {
                arguments = bundleOf(Constants.Extra.DRAMA to drama)
            }.show(fragmentManager)
        }
    }

    private val shareLauncher by lazy { (parentFragment as? ShareLauncher) ?: (activity as? ShareLauncher) }
    private val drama by lazy { arguments?.getParcelableValue(Constants.Extra.DRAMA, ShortDramaInfo::class.java) }
    private val shareManager by lazy { ShareManager(activity) }
    private val wxShareTitle: String?
        get() {
            return "${drama?.dramasSeries?.seriesName}丨第${drama?.workNo}章"
        }
    private val wxShareContent: String?
        get() {
            return "来金果一键创作，解锁你的爆款小说漫"
        }
    private val pyqShareTitle: String?
        get() {
            return "${wxShareTitle}。${wxShareContent}"
        }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnWx -> {
                shareLauncher?.uploadVideo(false) {
                    doShare(SHARE_MEDIA.WEIXIN)
                }
            }

            R.id.btnPyq -> {
                shareLauncher?.uploadVideo(false) {
                    doShare(SHARE_MEDIA.WEIXIN_CIRCLE)
                }
            }

            R.id.btnCopy -> {
                shareLauncher?.uploadVideo(false) {
                    mRealVM.getShareUrl { url -> copyUrl(url) }
                }
            }

            R.id.btnDownload -> {
                if (drama?.isMine == true)
                    DownloadTypeDialog.show(this, drama?.dramasSeries?.seriesCoverUrl, drama?.dramasSeries?.seriesName, drama?.artist?.nickName)
                else "暂不支持下载Ta人作品".showToast()
            }

            R.id.btnReport -> {
                ReportActivity.start(1, drama?.dramasId, drama?.artist?.userToken)
            }

            R.id.btnEdit -> {
                activity?.supportFragmentManager?.let {
                    ShortInfoEditorDialog.show(
                        it,
                        drama?.dramasId,
                        drama?.dramasName,
                        drama?.scriptIntro
                    )
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    private fun copyUrl(url: String) {
        val copyText = "$pyqShareTitle\n$url"
        ClipboardUtils.copyText(copyText)
        "复制成功".showToast()
    }

    private fun doShare(shareMedia: SHARE_MEDIA) {
        mRealVM.getShareUrl { shareUrl -> share(shareUrl, shareMedia) }
    }

    private fun share(shareUrl: String, shareMedia: SHARE_MEDIA) {
        mRealVM.loading.set(true)
        val share = { imageBitmap: Bitmap? ->
            mRealVM.loading.set(false)
            shareManager.doShare(
                if (shareMedia == SHARE_MEDIA.WEIXIN_CIRCLE) pyqShareTitle else wxShareTitle,
                wxShareContent,
                imageBitmap,
                R.drawable.ic_default_cover,
                shareUrl,
                shareMedia,
                this
            )
        }
        getImageBitmap(drama?.dramasSeries?.seriesCoverUrl) { imageBitmap -> share.invoke(imageBitmap) }
    }

    override fun onStart(p0: SHARE_MEDIA?) {
    }

    override fun onResult(p0: SHARE_MEDIA?) {
        dismissAllowingStateLoss()
    }

    override fun onError(p0: SHARE_MEDIA?, p1: Throwable?) {
        if (p1 is KsShareException) {
            p1.message ?: "分享失败"
        } else {
            "分享失败"
        }.showToast()
        dismissAllowingStateLoss()
    }

    override fun onCancel(p0: SHARE_MEDIA?) {
        "已取消分享".showToast()
        dismissAllowingStateLoss()
    }

    override fun fragmentManager(): FragmentManager? = childFragmentManager

    override fun downloadVideo(watermark: Boolean) {
        shareLauncher?.downloadVideo(watermark)
        dismissAllowingStateLoss()
    }

    interface ShareLauncher : DownloadTypeDialog.DownloadTypeLauncher {
        fun uploadVideo(watermark: Boolean, onUpload: (url: String) -> Unit)
    }
}