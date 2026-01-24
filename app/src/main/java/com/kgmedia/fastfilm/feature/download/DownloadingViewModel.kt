package com.kgmedia.fastfilm.feature.download

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.viewmodel.AppViewModel

/**
 * 下载中-进度
 */
class DownloadingViewModel : AppViewModel() {
    val statusHolder by lazy { ObservableField<DownloadStatusHolder>() }

    interface DownloadStatusHolderFactory {
        fun getStatusHolder(): DownloadStatusHolder?
    }

    interface DownloadStatusHolder {
        val downloadProgress: ObservableInt
        fun cancelDownload()
    }
}