package com.shiyuan.fastclaw.core.manager

import com.umeng.socialize.UMShareListener
import com.umeng.socialize.bean.SHARE_MEDIA
import java.lang.ref.WeakReference

class WeakReferenceUMShareListener(listener: UMShareListener?) : UMShareListener {
    private val shareListener = WeakReference(listener)
    override fun onStart(p0: SHARE_MEDIA?) {
        shareListener.get()?.onStart(p0)
    }

    override fun onResult(p0: SHARE_MEDIA?) {
        shareListener.get()?.onResult(p0)
    }

    override fun onError(p0: SHARE_MEDIA?, p1: Throwable?) {
        shareListener.get()?.onError(p0, p1)
    }

    override fun onCancel(p0: SHARE_MEDIA?) {
        shareListener.get()?.onCancel(p0)
    }
}