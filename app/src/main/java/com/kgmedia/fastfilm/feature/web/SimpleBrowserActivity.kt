package com.kgmedia.fastfilm.feature.web

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.NetworkUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.gyf.immersionbar.ImmersionBar
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.BuildConfig
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.downloadFile
import com.kgmedia.fastfilm.common.ext.removeHttpUrlParam
import com.kgmedia.fastfilm.common.ext.selectSinglePhotoFromAlbums
import com.kgmedia.fastfilm.common.jsbridge.WebViewJavascriptBridge
import com.kgmedia.fastfilm.common.jsbridge.data.DownLoadImageData
import com.kgmedia.fastfilm.common.permissions.PermissionsUtils
import com.kgmedia.fastfilm.common.permissions.requestImages2VideosPermissions
import com.kgmedia.fastfilm.common.permissions.requestRead2WritePermissions
import com.kgmedia.fastfilm.common.permissions.requestRxPermissions
import com.kgmedia.fastfilm.common.utils.ImageSaveHelper
import com.kgmedia.fastfilm.common.utils.IntentUtils
import com.kgmedia.fastfilm.common.utils.WXUtils
import com.kgmedia.fastfilm.core.CookieUtils
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.ext.createAppPayment
import com.kgmedia.fastfilm.core.ext.onUserStatusChanged
import com.kgmedia.fastfilm.core.ext.openWithBrowser
import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.manager.ShareManager
import com.kgmedia.fastfilm.core.model.H5PayInfo
import com.kgmedia.fastfilm.core.model.PermissionEntity
import com.kgmedia.fastfilm.core.model.PreviewImageEntity
import com.kgmedia.fastfilm.core.model.ResultEntity
import com.kgmedia.fastfilm.core.model.UriEntity
import com.kgmedia.fastfilm.core.network.ObjectTypeAdapter
import com.kgmedia.fastfilm.core.network.apis.AppHeader
import com.kgmedia.fastfilm.databinding.ActivityBrowserBinding
import com.kgmedia.fastfilm.feature.PhotoPagerActivity
import com.kgmedia.fastfilm.router.AppNavigator
import com.kgmedia.fastfilm.router.AppRouter
import com.kgmedia.fastfilm.widget.AndroidBug5497Workaround
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.Session
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.paddingStatusBar
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.ext.statusBarHeight
import com.nhsd.basic.utils.Logger
import com.nhsd.mattise.Matisse
import com.tencent.bugly.crashreport.CrashReport
import com.umeng.analytics.MobclickAgent
import org.json.JSONObject

class SimpleBrowserActivity : BaseViewModelActivity<ActivityBrowserBinding, BrowserViewModel>() {

    companion object {
        private const val REQUEST_CODE_PHOTO_PICKER = 0x999
        private const val TAG = "SimpleBrowserActivity"

        @JvmStatic
        fun launch(context: Context?, url: String?) {
            val darkStyle = url?.contains("darkStyle=1") ?: false
            val interceptBack = url?.contains("interceptBack=1") ?: false
            val rawUrl = url?.removeHttpUrlParam("darkStyle", "interceptBack")
            launch(context, rawUrl, interceptBack = interceptBack, darkStyle = darkStyle)
        }

        @JvmStatic
        fun launch(context: Context?, url: String?, interceptBack: Boolean?) {
            launch(context, url, interceptBack, false)
        }

        @JvmStatic
        fun launch(context: Context?, url: String?, interceptBack: Boolean?, darkStyle: Boolean) {
            var title: String? = null
            var urlParam: String? = url
            if (url != null && url.contains("nav=1")) {
                urlParam = url + "&H=" + context?.statusBarHeight
            } else {
                title = Session.getInstance().appName
            }
            launchInternal(
                context,
                urlParam,
                title,
                interceptBack = interceptBack,
                darkStyle = darkStyle
            )
        }

        @JvmStatic
        fun launch(context: Context?, url: String?, title: String?) {
            launchInternal(context, url, title, interceptBack = false)
        }

        private fun launchInternal(
            context: Context?,
            url: String?,
            title: String?,
            pushFlag: String? = null,
            interceptBack: Boolean? = false,
            darkStyle: Boolean = true
        ) {
            context ?: return
            url ?: return
            Logger.i("http", "url=$url")
            val finalUrl = if (!url.contains("ran=")) {
                if (url.contains("?")) "$url&ran=${App.instance.ran}"
                else "$url?ran=${App.instance.ran}"
            } else url

            Intent(context, SimpleBrowserActivity::class.java).apply {
                putExtra(Constants.Extra.URL, finalUrl)
                if (Constants.Extra.PUSH == pushFlag) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                putExtra(Constants.Extra.INTERCEPT_BACK, interceptBack)
                putExtra(Constants.Extra.TITLE, title)
                putExtra(Constants.Extra.DARK_STYLE, darkStyle)
                context.startActivity(this)
            }
            Logger.d(TAG, "url:$finalUrl")
        }
    }

    //    private val mShareManager by lazy { ShareManager(this) }
    private var bridge: WebViewJavascriptBridge? = null
    private var gson: Gson? = null

    private var mUploadCallbackBelow: ValueCallback<Uri>? = null
    private var mUploadCallbackAboveL: ValueCallback<Array<Uri?>?>? = null

    private val mWebViewClient = object : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            // 在wifi网络条件下预加载
            if (NetworkUtils.isWifiConnected()) {
                view?.evaluateJavascript(
                    """
    (function() {
        try {
            if (!document.head) return;
            var links = document.querySelectorAll('link[rel="preload"]');
            for (var i = 0; i < links.length; i++) {
                var asAttr = links[i].getAttribute('as');
                if (asAttr === 'style') {
                    var href = links[i].href;
                    if (!document.querySelector('link[href="' + href + '"]')) {
                        var newLink = document.createElement('link');
                        newLink.rel = 'stylesheet';
                        newLink.href = href;
                        document.head.appendChild(newLink);
                    }
                }
            }
        } catch(e) {
            console.error("Preload stylesheet injection failed", e);
        }
    })();
""".trimIndent(), null
                )
            }
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            mRealVM.isLoading.value = false
            binding.viewAnimator.displayedChild = if (mRealVM.isLoadError.value == true) 1 else 0
            binding.progressbar.isVisible = false
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url.isNullOrEmpty()) return true
            if (url.contains("newView")) {
                launch(this@SimpleBrowserActivity, url, mRealVM.interceptBack, mRealVM.darkStyle)
                return true
            }
            return !url.startsWith("http") || super.shouldOverrideUrlLoading(view, url)
        }

        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            mRealVM.isLoadError.value = true
            mRealVM.isLoading.value = false
            binding.viewAnimator.displayedChild = 1
            binding.progressbar.isVisible = false
            binding.circularButton.isVisible = true
            binding.netPromptView.setText(R.string.page_network_error)
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.proceed()
        }
    }

    private val mWebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if (newProgress == 100) {
                binding.progressbar.isVisible = false
            } else {
                if (!binding.progressbar.isVisible) binding.progressbar.isVisible = true
                binding.progressbar.progress = newProgress
            }
            CrashReport.setJavascriptMonitor(binding.webview, true)
            super.onProgressChanged(view, newProgress)
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            if (Session.getInstance().appName != title && !title.isNullOrEmpty() && view?.url?.contains(title) == false && mRealVM.title?.isNotEmpty() == true) {
                mRealVM.mToolbarVM.mTitle.set(title)
            }
        }

        override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri?>?>?, fileChooserParams: FileChooserParams?): Boolean {
            mUploadCallbackAboveL = filePathCallback
            choosePic()
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidBug5497Workaround.assistActivity(this)
        mRealVM.paymentClient = createAppPayment()
//        mRealVM.shareManagerFactory = this
//        mRealVM.getFragmentManager = { supportFragmentManager }
        initUI()
        setupObservers()
        setTrans()
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun initUI() {
        binding.progressLayout.paddingStatusBar()

        if (mRealVM.darkStyle) {
            binding.root.setBackgroundColor(resources.getColor(R.color.bg_color_1_dark))
        }

        binding.circularButton.setOnClickListener {
            if (mRealVM.isLoading.value != true) {
                mRealVM.isLoadError.value = false
                mRealVM.isLoading.value = true
                binding.webview.loadUrl("javascript:window.location.reload(true)")
                binding.progressbar.isVisible = true
                binding.circularButton.isVisible = false
            }
        }

        binding.webview.apply {
            settings.javaScriptEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.mediaPlaybackRequiresUserGesture = false
            if (App.instance.hasAgree()) {
                val userAgent =
                    "${settings.userAgentString} Networking Gang/${Session.getInstance().versionName}"
                settings.apply {
                    userAgentString = userAgent
                    cacheMode = WebSettings.LOAD_DEFAULT
                    domStorageEnabled = true
                    allowFileAccess = true
                }

            }


            setBackgroundColor(0)
            background?.alpha = 0
            setDownloadListener { url, _, _, _, _ ->
                requestRead2WritePermissions(
                    R.string.sdcard_permission_description
                ) { allGranted ->
                    if (!allGranted) return@requestRead2WritePermissions
                    downloadFile(url)
                }
            }
        }

        binding.webview.webChromeClient = mWebChromeClient
        bridge = WebViewJavascriptBridge(
            this,
            binding.webview,
            UserServerHandler()
        ).apply {
            setCustomWebViewClient(mWebViewClient)
        }

        if (App.instance.hasAgree()) {
            mRealVM.url?.let { CookieUtils.syncCookie(it, "userInfo", User.get().userInfoStr) }
        }

        loadingUrl()
        registerHandle()

    }

    private fun setupObservers() {
//        mRealVM.showWaitingDialog.observe(this) { trackId ->
//            trackId?.let {
//                WaitingDialog.newInstance(it).apply {
//                    setOnDismissListener { finish() }
//                    show(supportFragmentManager)
//                }
//            }
//        }

        mRealVM.openWebUrl.observe(this) { url ->
            url?.openWithBrowser()
        }
        mRealVM.payStatusCallback.observe(this) { info ->
            info?.let { bridge?.callHandler("payStatusCallback", GsonUtils.toJson(it)) }
        }
        observeEvents {
            onUserStatusChanged {
                mRealVM.url?.let { CookieUtils.syncCookie(it, "userInfo", User.get().userInfoStr) }
                binding.webview.reload()
            }
        }
    }

    override fun onBackPressed() {
        if (mRealVM.isLoading.value != true &&
            mRealVM.isLoadError.value != true &&
            mRealVM.interceptBack
        ) {

            bridge?.callHandler("confirmBack", null) { data ->
                if (data == null || data.contains("false")) {
                    App.instance.post { super.onBackPressed() }
                }
            }
        } else {
            super.onBackPressed()
        }
    }

//    override fun onStart(share_media: SHARE_MEDIA?) {}
//    override fun onResult(share_media: SHARE_MEDIA?) = "分享成功！".showToast()
//    override fun onError(share_media: SHARE_MEDIA?, throwable: Throwable?) = "分享失败！".showToast()
//    override fun onCancel(share_media: SHARE_MEDIA?) {}

    inner class UserServerHandler : WebViewJavascriptBridge.WVJBHandler {
        override fun handle(data: String?, jsCallback: WebViewJavascriptBridge.WVJBResponseCallback?) {
            Logger.d(TAG, "Received message from javascript: $data")
            jsCallback?.callback("Java said:Right back atcha")
            bridge?.send("I expect a response!") { responseData ->
                Logger.d("test", "Got response! $responseData")
            }
            bridge?.send("Hi")
        }
    }

    private fun registerHandle() {
        bridge?.apply {
            registerHandler("back") { _, _ -> super.onBackPressed() }
            registerHandler("backIntercept") { data, _ ->
                if (data == null || !data.contains("true")) {
                    super.onBackPressed()
                }
            }
            registerHandler("login") { _, _ -> AppNavigator.checkLoginIntercepted(true) }
            registerHandler("city") { _, jsCallback ->
                if (!IntentUtils.isLocationServiceEnable(this@SimpleBrowserActivity)) {
                    IntentUtils.startOpenGPSSetting(this@SimpleBrowserActivity)
                    jsCallback?.callback("")
                    return@registerHandler
                }
                requestLocationPermission(jsCallback, false)
            }
            registerHandler("saveImage") { data, _ ->
                val src = Gson().fromJson(data, DownLoadImageData::class.java)
                if (!TextUtils.isEmpty(src.src)) {
                    cacheImage(src.src)
                } else {
                    showToast(R.string.download_image_failed)
                }
            }
//            registerHandler("webShare") { data, _ ->
//                val webEntity = Gson().fromJson(data, WebEntity::class.java)
//                WechatShareDialog.shareWebUrl(webEntity)
//            }
            registerHandler("schemeJump") { data, _ ->
                val uriEntity = Gson().fromJson(data, UriEntity::class.java)
                AppRouter.jump(uriEntity.uri)
            }
            registerHandler("getAppVersion") { _, jsCallback ->
                jsCallback?.callback(BuildConfig.VERSION_NAME)
            }
            registerHandler("getAppHeader") { _, jsCallback ->
                if (gson == null) {
                    gson = GsonBuilder().registerTypeAdapter(
                        Object::class.java,
                        ObjectTypeAdapter()
                    ).create()
                }
                jsCallback?.callback(gson!!.toJson(AppHeader()))
            }
            registerHandler("previewImage") { data, _ ->
                val entity = Gson().fromJson(data, PreviewImageEntity::class.java)
                PhotoPagerActivity.launch(
                    this@SimpleBrowserActivity,
                    ArrayList(entity.urls),
                    entity.current
                )
            }
            registerHandler("openPermission") { data, jsCallback ->
                val entity = Gson().fromJson(data, PermissionEntity::class.java)
                when (entity.name) {
                    "location" -> {
                        requestRxPermissions(
                            getString(R.string.location_permission_description), Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) { granted ->
                            val result =
                                ResultEntity(if (granted) ResultEntity.SUCCESS else ResultEntity.FAILED)
                            jsCallback?.callback(Gson().toJson(result))
                        }
                    }

                    "album" -> requestImages2VideosPermissions(
                        R.string.sdcard_permission_description.getResString(),
                        withVideo = true,
                        openSelect = true
                    ) { granted ->
                        val result =
                            ResultEntity(if (granted) ResultEntity.SUCCESS else ResultEntity.FAILED)
                        jsCallback?.callback(Gson().toJson(result))
                    }

                    "camera" -> {
                        requestRxPermissions(
                            getString(R.string.sdcard_permission_description), *PermissionsUtils.getCameraPermission()
                        ) { granted ->
                            val result =
                                ResultEntity(if (granted) ResultEntity.SUCCESS else ResultEntity.FAILED)
                            jsCallback?.callback(Gson().toJson(result))
                        }
                    }
                }
            }
//            registerHandler("getCityCode") { _, jsCallback ->
//                val result = ResultEntity(ResultEntity.SUCCESS, User.get().cityCode)
//                jsCallback?.callback(toJson(result))
//            }
//            registerHandler("getLocation") { data, jsCallback ->
//                val getLocationEntity = Gson().fromJson(data, GetLocationEntity::class.java)
//                if (getLocationEntity.isOpenPermission) {
//                    if (!IntentUtils.isLocationServiceEnable(this@SimpleBrowserActivity)) {
//                        IntentUtils.startOpenGPSSetting(this@SimpleBrowserActivity)
//                        jsCallback?.callback(toJson(ResultEntity(ResultEntity.FAILED)))
//                        return@registerHandler
//                    }
//                    requestLocationPermission(jsCallback, true)
//                } else {
//                    val result = ResultEntity(
//                        ResultEntity.SUCCESS,
//                        "${User.get().longitude},${User.get().latitude}"
//                    )
//                    jsCallback?.callback(toJson(result))
//                }
//            }
//            registerHandler("startComposeChristmasMusic") { data, jsCallback ->
//                val contentData = Gson().fromJson(data, ContentData::class.java)
//                contentData?.let { it.content?.let { it1 -> mRealVM.composeChristmasMusic(it1) } }
//            }
//            registerHandler("startComposeDebutMusic") { data, jsCallback ->
//                val stageInfo = Gson().fromJson(data, StageInfo::class.java)
//                mRealVM.composeDebutMusic(stageInfo)
//            }
//            registerHandler("startComposeCreativeMusic") { data, jsCallback ->
//                val creativeBody = Gson().fromJson(data, CreativeContentBody::class.java)
//                mRealVM.composeCreativeMusic(creativeBody) {
//                    jsCallback.callback("200")
//                }
//            }
//            registerHandler("inviteUserClick") { data, _ ->
//                val entity = Gson().fromJson(data, InviteUrlEntity::class.java)
//                mShareManager.doShare(
//                    "只需一句话，为你生成歌",
//                    "来快歌创作你的专属歌曲，还有各种写歌现金奖励等你哦",
//                    getBitmap(R.mipmap.ic_launcher),
//                    R.mipmap.ic_launcher,
//                    entity.inviteUrl,
//                    SHARE_MEDIA.WEIXIN,
//                    this@SimpleBrowserActivity
//                )
//                EventManager.onAppEvent(
//                    AppEvent.INVITE_FRIEND_INVITE_BUTTON_CLICK,
//                    null,
//                    "{\"isWeb\":true}"
//                )
//            }
            registerHandler("webPay") { data, jsCallback ->
                try {
                    mRealVM.orderNo = null
                    val info = GsonUtils.fromJson(data, H5PayInfo::class.java)
                    mRealVM.startPay(info)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            registerHandler("openWebUrl") { data, _ ->
                try {
                    val uriEntity = GsonUtils.fromJson(data, UriEntity::class.java)
                    uriEntity.uri.openWithBrowser()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            //跳转微信客服
            //{
            //customerServiceJmpUrl: 跳转地址，
            //enterpriseId： id
            //}
            registerHandler("openWeChatService") { data, _ ->
                try {
                    val json = JSONObject(data)
                    WXUtils.customerServiceChat(this@SimpleBrowserActivity, json.optString("customerServiceJmpUrl"), json.optString("enterpriseId"))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            //埋点上报
            // {
            //  "event_name": 埋点名称,
            //  "params": 埋点参数（json）
            //}
            registerHandler("trackEvent") { data, _ ->
                try {
                    val json = JSONObject(data)
                    val code = json.getString("event_name")
                    val params = json.optJSONObject("params")?.let {
                        val map = mutableMapOf<String, String?>()
                        it.keys().forEach { key -> map[key] = it.optString(key) }
                        map
                    }
                    EventManager.onAppTrack(code, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun <T> toJson(result: T): String = Gson().toJson(result)

    private fun requestLocationPermission(
        jsCallback: WebViewJavascriptBridge.WVJBResponseCallback?,
        useJson: Boolean
    ) {
        requestRxPermissions(
            getString(R.string.location_permission_description), Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) { granted ->
            if (granted) {
                // TODO: Start location if needed
            } else {
                if (useJson) {
                    jsCallback?.callback(toJson(ResultEntity(ResultEntity.FAILED)))
                } else {
                    jsCallback?.callback("")
                }
            }
        }
    }

    private fun cacheImage(url: String) {
        ImageSaveHelper.saveImage(this, url)
    }

    private fun choosePic() {
        requestImages2VideosPermissions(
            getString(R.string.sdcard_permission_description),
            withVideo = true,
            openSelect = true
        ) { granted ->
            if (granted) {
                selectSinglePhotoFromAlbums(
                    null,
                    REQUEST_CODE_PHOTO_PICKER,
                    capture = true,
                    useEmbeddedCamera = true
                )
            }
        }
    }

    private fun loadingUrl() {
        mRealVM.url?.let {
            binding.webview.loadUrl(it)
            mRealVM.isLoading.value = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PHOTO_PICKER -> handlePhotoPickerResult(resultCode, data)
        }
        ShareManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handlePhotoPickerResult(resultCode: Int, data: Intent?) {
        if (mUploadCallbackBelow == null && mUploadCallbackAboveL == null) return

        if (mUploadCallbackBelow != null) {
            val result = if (resultCode != RESULT_OK) null else Matisse.obtainResult(data)
            mUploadCallbackBelow?.onReceiveValue(result?.getOrNull(0))
            mUploadCallbackBelow = null
        } else if (mUploadCallbackAboveL != null) {
            val result = if (resultCode != RESULT_OK) null else Matisse.obtainResult(data)
            mUploadCallbackAboveL?.onReceiveValue(result?.toTypedArray())
            mUploadCallbackAboveL = null
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webview.apply {
            onResume()
            resumeTimers()
            settings.apply {
                mediaPlaybackRequiresUserGesture = false // 允许在非用户手势下播放媒体
                blockNetworkImage = false // 恢复图片加载
            }
            isVisible = false
            post { isVisible = true }
        }
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        binding.webview.apply {
            onPause()
            pauseTimers()
            // 仅禁用非必要的JS功能
            settings.apply {
                mediaPlaybackRequiresUserGesture = true // 恢复需要用户手势播放媒体
                blockNetworkImage = true // 暂停时阻止图片加载
            }
        }
        MobclickAgent.onPause(this)
    }

    private fun setTrans() {
        if (mRealVM.darkStyle) {
            binding.progressLayout.isVisible = false
            ImmersionBar.with(this)
                .statusBarDarkFont(false)
                .navigationBarColor(R.color.bg_color_1_dark)
                .init()
        } else {
            if (!TextUtils.isEmpty(mRealVM.title)) {
                ImmersionBar.with(this)
                    .statusBarDarkFont(true)
                    .navigationBarColor(R.color.white)
                    .navigationBarDarkIcon(true)
                    .init()
            } else {
                ImmersionBar.with(this)
                    .statusBarDarkFont(true)
                    .statusBarColor(R.color.transparent)
                    .init()
            }
        }
    }

    override fun onDestroy() {
        binding.webview.destroy()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webview.canGoBack()) {
            binding.webview.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

//    override fun getShareManager(): ShareManager {
//        return mShareManager
//    }
//
//    override fun getShareListener(): UMShareListener? {
//        return this
//    }
//
//    override fun getStatusHolder(): DownloadingViewModel.DownloadStatusHolder? {
//        return if (isActive) mRealVM else null
//    }

}