package com.kgmedia.fastfilm.feature.play.more

import android.content.Intent
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.base.viewmodel.BaseShareViewModel
import com.kgmedia.fastfilm.common.permissions.requestRead2WritePermissions
import com.kgmedia.fastfilm.core.manager.ShareManager
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.feature.download.DownloadingDialog
import com.kgmedia.fastfilm.feature.download.DownloadingViewModel
import com.kgmedia.fastfilm.feature.series.share.EpisodeShareDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.showToast
import com.nhsd.share.KsShareException
import com.umeng.socialize.UMShareListener
import com.umeng.socialize.bean.SHARE_MEDIA

/**
 * 分享短剧
 */
abstract class ShareShortDramaActivity<V : ViewDataBinding, VM : ShareShortDramaViewModel<*>> : BaseViewModelActivity<V, VM>(),
    BaseShareViewModel.ShareManagerFactory,
    UMShareListener,
    EpisodeShareDialog.ShareLauncher,
    DownloadingViewModel.DownloadStatusHolderFactory {
    private val mShareManager by lazy { ShareManager(this) }
    private var downloadingDialog: DownloadingDialog? = null
    protected abstract val exportVideoOverlay: View?

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        mRealVM.shareManagerFactory = this
        mRealVM.downloading.observe(this) {
            if (it) {
                if (downloadingDialog == null) {
                    downloadingDialog = DownloadingDialog.show(this)
                }
            } else {
                downloadingDialog?.dismissAllowingStateLoss()
                downloadingDialog = null
            }
        }
    }

    override fun getStatusHolder(): DownloadingViewModel.DownloadStatusHolder? = if (isActive) mRealVM else null

    override fun fragmentManager(): FragmentManager? = supportFragmentManager

    override fun downloadVideo(watermark: Boolean) {
        requestRead2WritePermissions {
            if (it) {
                mRealVM.share(saveToAlbum = true, overlay = if (watermark) exportVideoOverlay else null)
            }
        }
    }

    override fun uploadVideo(watermark: Boolean, onUpload: (String) -> Unit) {
        val shortDrama = mRealVM.shortDrama ?: return
        val videoUrl = shortDrama.dramasVideo?.videoUrl
        if (videoUrl.isNullOrEmpty()) {
            mRealVM.share(upload = true, onUpload = onUpload, overlay = if (watermark) exportVideoOverlay else null)
        } else {
            onUpload.invoke(videoUrl)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ShareManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun getShareManager(): ShareManager = mShareManager

    override fun getShareListener(): UMShareListener? = this

    override fun onStart(p0: SHARE_MEDIA?) {
    }

    override fun onResult(p0: SHARE_MEDIA?) {
    }

    override fun onError(p0: SHARE_MEDIA?, p1: Throwable?) {
        if (p1 is KsShareException) {
            p1.message ?: "分享失败"
        } else {
            "分享失败"
        }.showToast()
    }

    override fun onCancel(p0: SHARE_MEDIA?) {
        "已取消分享".showToast()
    }

    fun shareShortDrama(info: ShortDramaInfo?) {
        if (info == null) {
            return
        }
        mRealVM.sharingShortDramaInfo = info
        EpisodeShareDialog.show(this, info)
    }

}