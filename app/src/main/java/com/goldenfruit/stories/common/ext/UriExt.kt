package com.kgmedia.fastfilm.common.ext

import android.net.Uri
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.common.utils.RealPathUtil
import com.kgmedia.fastfilm.common.utils.Utils

fun Uri.getRealPath(): String? {
    return try {
        RealPathUtil.getRealPath(App.instance, this)
    } catch (e: Exception) {
        null
    }
}

fun Uri.getFileName(): String? {
    return try {
        Utils.getFileName(RealPathUtil.getRealPath(App.instance, this))
    } catch (e: Exception) {
        System.nanoTime().toString()
    }
}