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
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.permissions.requestRead2WritePermissions
import com.kgmedia.fastfilm.common.utils.IntentUtils
import com.kgmedia.fastfilm.common.utils.Utils
import com.kgmedia.fastfilm.core.manager.IRouter
import com.kgmedia.fastfilm.core.network.apis.ApiFactory
import com.kgmedia.fastfilm.core.network.apis.request.DramasIdBody
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorActivity
import com.kgmedia.fastfilm.feature.editor.EditStoryboardVideoActivity
import com.kgmedia.fastfilm.feature.play.DramasPlayPagerActivity
import com.kgmedia.fastfilm.feature.web.SimpleBrowserActivity.Companion.launch
import com.kgmedia.fastfilm.widget.LoadingDialog2
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext


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
        val uri = Uri.parse(schemeUri)
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
                if (AppNavigator.checkLoginIntercepted(true)) {
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
            //短剧作品，APP需判断作品当前状态跳转对应页面
            //fastfilm://shortDramas?dramasId=短剧id
            "shortDramas" -> {
                startShortDrama(uri.getQueryParameter("dramasId"))
            }
            "bookGenerated" -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                AppLaunchJump.launchJump(uri)
            }

            else -> {}
        }
    }

    fun openBrowser(context: Context, url: String?) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = url?.toUri()
        // 注意此处的判断intent.resolveActivity()可以返回显示该Intent的Activity对应的组件名
        // 官方解释 : Name of the component implementing an activity that can display the intent
        if (intent.resolveActivity(context.packageManager) != null) {
            val componentName = intent.resolveActivity(context.packageManager)
            context.startActivity(Intent.createChooser(intent, "请选择浏览器"))
        } else {
            // GlobalMethod.showToast(context, "链接错误或无浏览器");
        }
    }

    @JvmStatic
    fun startShortDrama(dramasId: String?) {
        if (TextUtils.isEmpty(dramasId)) {
            return
        }
        val loading = (topActivity as? FragmentActivity)?.supportFragmentManager?.let {
            val dialog = LoadingDialog2()
            dialog.show(it)
            dialog
        }
        MainScope().launchNoCrash(
            block = {
                val res = withContext(Dispatchers.IO) { ApiFactory.apiService.shortDramasStatus(DramasIdBody(dramasId)) }
                if (!res.isSuccess) {
                    res.msg?.showToast()
                    loading?.dismissAllowingStateLoss()
                    return@launchNoCrash
                }
                val info = res.data
                if (info?.publishStatus == 1) {
                    DramasPlayPagerActivity.start(info.dramasSeriesId)
                } else {
                    //作品状态 [0=预生成中(剧本大纲和分镜在生成中) 1=画面生成中 2=画面全部生成失败 3=画面部分生成失败 4=待选择角色 5=画面生成成功(未合成短剧) 6=已合成短剧 7=预生成成功(剧本大纲和分镜在生成成功)]
                    when (info?.workStatus) {
                        1, 2, 3, 5 -> {
                        }

                        else -> {}
                    }
                }
                loading?.dismissAllowingStateLoss()
            },
            onError = {
                loading?.dismissAllowingStateLoss()
            }
        )
    }
}