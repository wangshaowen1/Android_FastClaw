package com.kgmedia.fastfilm.feature.series.editinfo

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.kgmedia.fastfilm.App
import com.nhsd.mattise.MimeType
import com.nhsd.mattise.internal.utils.PathUtils

/**
 * 媒体文件，图片或视频
 */
class MediaItem(
    val localUri: Uri? = null,
    var localPath: String? = localUri?.let { PathUtils.getPath(App.instance, it) },
    var remoteUrl: String? = null,
    val mimeType: String? = null
) : Parcelable {

    /**
     * 预览URL，本地优先
     */
    val previewUrl get() = localPath ?: remoteUrl

    /**
     * 上传URL，网络连接优先，避免重复上传
     */
    val uploadPath get() = remoteUrl ?: localPath

    fun isImage(): Boolean {
        return MimeType.isImage(mimeType)
    }

    fun isGif(): Boolean {
        return MimeType.isGif(mimeType)
    }

    fun isVideo(): Boolean {
        return MimeType.isVideo(mimeType)
    }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Uri::class.java.classLoader),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(localUri, flags)
        parcel.writeString(localPath)
        parcel.writeString(remoteUrl)
        parcel.writeString(mimeType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaItem> {
        override fun createFromParcel(parcel: Parcel): MediaItem {
            return MediaItem(parcel)
        }

        override fun newArray(size: Int): Array<MediaItem?> {
            return arrayOfNulls(size)
        }
    }

}