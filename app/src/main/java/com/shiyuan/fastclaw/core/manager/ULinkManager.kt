package com.shiyuan.fastclaw.core.manager

import android.content.Context
import android.net.Uri
import com.nhsd.basic.utils.Logger
import com.shiyuan.fastclaw.core.Config
import com.umeng.umlink.MobclickLink
import com.umeng.umlink.UMLinkListener
import java.util.HashMap

object ULinkManager {
    private const val TAG = "ULink"
    private const val KEY_INVITE_CODE = "inviteCode"
    private const val KEY_INVITE_SCENE = "inviteScene"

    private val listener = object : UMLinkListener {
        override fun onLink(path: String?, queryMap: HashMap<String, String>?) {
            storeInviteAttribution(queryMap, null)
        }

        override fun onInstall(params: HashMap<String, String>?, uri: Uri?) {
            storeInviteAttribution(params, uri)
        }

        override fun onError(error: String?) {
            Logger.i("$TAG error=$error")
        }
    }

    @JvmStatic
    fun init(context: Context) {
        MobclickLink.setUseClipboard(true)
        MobclickLink.getInstallParams(context.applicationContext, true, listener)
    }

    @JvmStatic
    fun handleUri(context: Context, uri: Uri?) {
        if (uri == null) return
        cacheUri(uri)
        MobclickLink.handleUMLinkURI(context, uri, listener)
    }

    @JvmStatic
    fun cacheUri(uri: Uri?) {
        storeInviteAttribution(null, uri)
    }

    private fun storeInviteAttribution(params: Map<String, String>?, uri: Uri?) {
        val inviteCode = params?.getValidValue(KEY_INVITE_CODE)
            ?: uri.getValidQueryParameter(KEY_INVITE_CODE)
        val inviteScene = params?.getValidValue(KEY_INVITE_SCENE)
            ?: uri.getValidQueryParameter(KEY_INVITE_SCENE)
        if (inviteCode == null && inviteScene == null) {
            return
        }
        Config.get().storeInviteAttribution(inviteCode, inviteScene)
    }

    private fun Map<String, String>.getValidValue(key: String): String? {
        return this[key]?.takeIf { it.isNotBlank() }
    }

    private fun Uri?.getValidQueryParameter(key: String): String? {
        return try {
            this?.getQueryParameter(key)?.takeIf { it.isNotBlank() }
        } catch (e: UnsupportedOperationException) {
            null
        }
    }
}
