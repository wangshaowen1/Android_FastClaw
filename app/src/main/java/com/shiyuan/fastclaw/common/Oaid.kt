package com.shiyuan.fastclaw.common


import com.bytedance.applog.AppLog
import com.bytedance.applog.IOaidObserver
import com.github.gzuliyujiang.oaid.DeviceID
import com.shiyuan.fastclaw.App
import kotlin.text.isNullOrEmpty

object Oaid : IOaidObserver {
    private var oaid: String? = null

    @JvmStatic
    fun init() {
        if (App.instance.hasAgree()) {
            AppLog.setOaidObserver(this)
        }
    }

    override fun onOaidLoaded(oaid: IOaidObserver.Oaid) {
        if (!oaid.id.isNullOrEmpty()) {
            this.oaid = oaid.id
        }
    }

    @JvmStatic
    fun getOaid(): String {
        val oaid = oaid
        //if (oaid.isNullOrEmpty()) {
        //    this.oaid = EventReporter.oaid ?: EventReporterV2.oaid
        //}
        if (oaid.isNullOrEmpty()) {
            return DeviceID.getOAID()
        }
        return oaid
    }
}