package com.kgmedia.fastfilm.feature.play.more

import com.kgmedia.fastfilm.base.viewmodel.AppViewModel
import com.kgmedia.fastfilm.common.Constants

/**
 * 下载方式
 */
class DownloadTypeViewModel : AppViewModel() {
    val cover by lazy { mBundle.getString(Constants.Extra.COVER) }
    val title by lazy { mBundle.getString(Constants.Extra.TITLE) }
    val name by lazy { mBundle.getString(Constants.Extra.NAME) }
}