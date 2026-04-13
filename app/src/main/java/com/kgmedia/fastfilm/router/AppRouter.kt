package com.kgmedia.fastfilm.router

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.BuildConfig
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.permissions.requestRead2WritePermissions
import com.kgmedia.fastfilm.common.utils.IntentUtils
import com.kgmedia.fastfilm.common.utils.Utils
import com.kgmedia.fastfilm.core.manager.IRouter
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity.Companion.launch
import com.nhsd.basic.utils.Logger
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram
import com.tencent.mm.opensdk.openapi.WXAPIFactory

object AppRouter : IRouter {

    override fun openFromPush(context: Context, uri: String?) {
        uri?.let {
            AppLaunchJump.launchJump(uri.toUri())
        }
    }

    override fun navigate(context: Context, path: String?) {
        path?.let { jump(it) }
    }

    @JvmStatic
    fun jump(context: Context, schemeUri: String, openBrowser: Boolean) {
        if (openBrowser && schemeUri.startsWith("http")) {
            openBrowser(context, schemeUri)
        } else {
            jump(schemeUri)
        }
    }

    @JvmStatic
    fun jump(schemeUri: String?) {
        if (TextUtils.isEmpty(schemeUri)) {
            return
        }
        val uri = schemeUri?.toUri()
        if (uri != null) {
            var context: Context? = ActivityUtils.getTopActivity()
            if (context == null) {
                context = ActivityUtils.getTopActivity()
            }
            if (Constants.APP_SCHEME == uri.scheme) {
                jump(uri)
            } else if (schemeUri!!.endsWith(".apk")) {
                if (context is Activity) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || Utils.hasPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    ) {
                        IntentUtils.startDownload(context, schemeUri)
                    } else {
                        if (!Utils.isActivityFinishing(context)) {
                            val finalContext: Context? = context
                            (context as FragmentActivity).requestRead2WritePermissions(
                                context.getString(
                                    R.string.sdcard_permission_description
                                )
                            ) { allGranted: Boolean ->
                                if (allGranted && !Utils.isActivityFinishing(finalContext)) {
                                    IntentUtils.startDownload(finalContext, schemeUri)
                                }
                                null
                            }
                        }
                    }
                }
            } else if (schemeUri.startsWith("http")) {
                val target = uri.getQueryParameter("ff_jump_mode")
                if (target == "external") {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    topActivity?.startActivity(intent)
                    return
                }
                launch(context, schemeUri)
            }
        }
    }


    @JvmStatic
    fun jump(uri: Uri) {
        val host = uri.host
        Logger.i("host " + uri.host + ", uri=" + uri.toString())
        if (TextUtils.isEmpty(host)) {
            return
        }

        when (host) {
            "wechatMini" -> {//fastfilm://wechatMini?wechatMiniId=%s&type=%s&path=%s
                startWechatMini(uri.getQueryParameter("wechatMiniId"), uri.getQueryParameter("path"), uri.getQueryParameter("type")?.toIntOrNull())
            }

            else -> {}
        }
    }

    fun startWechatMini(wechatMiniId: String?, path: String?, type: Int? = null) {
        val api = WXAPIFactory.createWXAPI(App.instance, BuildConfig.WX_APP_ID)
        val req = WXLaunchMiniProgram.Req()
        req.userName = wechatMiniId // 填小程序原始id "gh_c2a64b1bcb27";
        req.path = path ?: ""//拉起小程序页面的可带参路径，不填默认拉起小程序首页，对于小游戏，可以只传入 query 部分，来实现传参效果，如：传入 "?foo=bar"。
        req.miniprogramType = type ?: WXLaunchMiniProgram.Req.MINIPTOGRAM_TYPE_RELEASE
        api.sendReq(req)
    }

    fun openBrowser(context: Context, url: String?) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = url?.toUri()
        // 注意此处的判断intent.resolveActivity()可以返回显示该Intent的Activity对应的组件名
        // 官方解释 : Name of the component implementing an activity that can display the intent
        if (intent.resolveActivity(context.packageManager) != null) {
            val componentName = intent.resolveActivity(context.packageManager)
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.select_browser)))
        } else {
            // GlobalMethod.showToast(context, "链接错误或无浏览器");
        }
    }

}