package com.kgmedia.fastfilm.feature.creator

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.showToast

/**
 */
class StoryUploadViewModel : AppRepositoryViewModel<AppRepository>() {

    val fileName by lazy { ObservableField<String>() }
    val uploadStatus by lazy { ObservableInt(STATE_NONE) }
    val uploadErrorType by lazy { ObservableInt() }
    val uploadProgress by lazy { ObservableField<String>() }

    companion object {
        const val STATE_NONE = 0
        const val STATE_UPLOADING = 1
        const val STATE_UPLOAD_SUCCESS = 2
        const val STATE_UPLOAD_FAIL = 3

        const val UPLOAD_ERROR_TYPE_FILE_TOO_LARGE = 1
        const val UPLOAD_ERROR_TYPE_OTHER = 2
    }

    fun getImageIcon(state: Int?): Drawable? {
        return when (uploadStatus.get()) {
            STATE_NONE -> R.drawable.img_text_upload
            STATE_UPLOAD_SUCCESS -> R.drawable.img_text
            STATE_UPLOAD_FAIL -> R.drawable.img_text_error
            else -> R.drawable.icon_loading2
        }.getResDrawable()
    }

    fun startUpload(path: String?) {
        if (path.isNullOrEmpty()) {
            "请选择文件".showToast()
            return
        }
        uploadStatus.set(STATE_UPLOADING)
        uploadProgress.set("0.0%")
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(path, OssClient.TYPE_ASSET, object : OnUploadListener() {

            override fun onUploadProgress(totalLength: Long, currentLength: Long) {
                val rawProgress = if (totalLength > 0) {
                    (currentLength.toDouble() / totalLength) * 100
                } else {
                    0.0
                }
                val progressText = String.format("%.1f%%", rawProgress.coerceIn(0.0, 100.0))
                uploadProgress.set(progressText)
            }

            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val url = uploadSuccess.firstOrNull()
                if (url.isNullOrEmpty()) {
                    onUploadFail(null)
                } else {
                    uploadStatus.set(STATE_UPLOAD_SUCCESS)
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                uploadErrorType.set(UPLOAD_ERROR_TYPE_OTHER)
                uploadStatus.set(STATE_UPLOAD_FAIL)
            }
        })
    }

}