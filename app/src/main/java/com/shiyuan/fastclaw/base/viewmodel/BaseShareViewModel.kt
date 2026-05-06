package com.shiyuan.fastclaw.base.viewmodel

import android.graphics.BitmapFactory
import android.view.View
import androidx.annotation.DrawableRes
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.toAlbum
import com.shiyuan.fastclaw.common.ext.toCache
import com.shiyuan.fastclaw.core.AppRepository
import com.shiyuan.fastclaw.core.ext.launchWithLoading
import com.shiyuan.fastclaw.core.manager.ShareManager
import com.nhsd.basic.ext.showToast
import com.umeng.socialize.UMShareListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 封装了分享（移除view层的引用，简化职责、减少耦合）
 * @author xiaoowuu
 */
open class BaseShareViewModel<T : AppRepository> : AppRepositoryViewModel<T>() {

    var shareManagerFactory: ShareManagerFactory? = null

    override fun onDestroy() {
        super.onDestroy()
        shareManagerFactory = null
    }

    interface ShareSourceFactory {
        suspend fun getShareSource(): File?
    }

    interface ShareManagerFactory {

        fun getShareManager(): ShareManager

        fun getShareListener(): UMShareListener?

    }

    /**
     * 保存邀请图片
     * @param view 需要保存为图片的View
     * @param res 需要保存为图片的Drawable Res
     * @param imageWidth 保存的图片宽度
     */
    fun saveImage(view: View? = null, @DrawableRes res: Int = 0, imageWidth: Float = 273f * 3) {
        launchWithLoading {
            val file = withContext(Dispatchers.IO) {
                if (view != null) {
                    view.toAlbum(scale = imageWidth / view.width)
                } else if (res != 0) {
                    BitmapFactory.decodeResource(App.instance.resources, res).toAlbum()
                } else {
                    null
                }
            }
            if (file?.exists() == true) {
                showToast(R.string.toast_save_success)
            } else {
                showToast(R.string.toast_save_fail)
            }
        }
    }
}

fun View.toShareSource(imageWidth: Float = 273f * 3): BaseShareViewModel.ShareSourceFactory {
    return object : BaseShareViewModel.ShareSourceFactory {
        override suspend fun getShareSource(): File? {
            return this@toShareSource.toCache(scale = imageWidth / this@toShareSource.width)
        }
    }
}