package com.shiyuan.fastclaw.core.manager

// TODO: Feature
import android.app.ProgressDialog
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blankj.utilcode.util.NetworkUtils
import com.shiyuan.fastclaw.common.ext.ioScope
import com.shiyuan.fastclaw.common.permissions.PermissionsUtils
import com.shiyuan.fastclaw.common.permissions.requestRxPermissions
import com.shiyuan.fastclaw.common.utils.FileUtils
import com.shiyuan.fastclaw.core.Config
import com.shiyuan.fastclaw.core.network.apis.ApiFactory
import com.shiyuan.fastclaw.core.network.apis.response.UpgradeData
import com.shiyuan.fastclaw.feature.upgrade.UpgradeDialogFragment
import com.shiyuan.fastclaw.service.DownloadWorker
import com.nhsd.basic.BaseActivity
import com.nhsd.basic.Session
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.manager.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UpgradeManager(private val context: BaseActivity) {

    private var mProgressDialog: ProgressDialog? = null
    var needForceUpdate = false
        private set
    private var dialog: UpgradeDialogFragment? = null
    private var isRequesting = false
    private val autoUpgrade = false
    private var upgradeData: UpgradeData? = null

    private var onUpgradeListener: OnUpgradeListener? = null

    interface OnUpgradeListener {
        fun onUpgrade(needUpgrade: Boolean)
    }

    init {
        // 替代 EventBus.register(this)
        observeEvents()
    }

    /**
     * 监听全局事件
     * 使用 Activity 的 lifecycleScope，确保生命周期安全
     */
    private fun observeEvents() {
        context.lifecycleScope.launch {
            // 当 Activity 至少处于 STARTED 状态时才接收事件 (避免后台弹窗崩溃)
            context.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 页面不在前台 → 缓存 sticky，返回前台时 getStickyEvent 取出
                FlowEventBus.getStickyEvent(DownloadWorker.DownloadApkEvent::class.java)?.let {
                    consumeDownloadApkEvent(it)
                }
               // 页面正在前台 → collect 立即收到
                FlowEventBus.events.collect { event ->
                    if (event is DownloadWorker.DownloadApkEvent) {
                        consumeDownloadApkEvent(event)
                    }
                }
            }
        }
    }

    private fun consumeDownloadApkEvent(event: DownloadWorker.DownloadApkEvent) {
        FlowEventBus.removeStickyEvent(DownloadWorker.DownloadApkEvent::class.java)
        onFinishDownload(event)
    }

    fun setOnUpgradeListener(onUpgradeListener: OnUpgradeListener?) {
        this.onUpgradeListener = onUpgradeListener
    }

    @Synchronized
    fun upgrade(showProgress: Boolean) {
        if (isRequesting) return
        if (showProgress) {
            showUpgradeDialog()
        }

        isRequesting = true

        ioScope.launchNoCrash {
            try {
                // 1. 请求接口（IO 线程）
                val data = withContext(Dispatchers.IO) {
                    ApiFactory.apiService.checkAppVersion().data
                }

                // 切回主线程处理 UI 与逻辑
                withContext(Dispatchers.Main) {
                    if (!context.isActive) {
                        onUpgradeListener?.onUpgrade(false)
                        return@withContext
                    }

                    if (data == null) {
                        onUpgradeListener?.onUpgrade(false)
                        if (showProgress) mProgressDialog?.dismiss()
                        return@withContext
                    }

                    upgradeData = data

                    val url = data.upgradeUrl
                    val md5 = data.md5Hash

                    if (!data.isNeedUpdate()) {
                        if (showProgress) {
                            "当前已经是最新版本".showToast()
                        }
                    } else {
                        needForceUpdate = data.isNeedForceUpdate()

                        val shouldShowUpgrade = needForceUpdate ||
                                showProgress ||
                                Config.get().shouldShowRecommendUpgrade(
                                    RECOMMEND_UPGRADE_REMIND_INTERVAL_MILLIS
                                )
                        if (!shouldShowUpgrade) {
                            onUpgradeListener?.onUpgrade(true)
                            return@withContext
                        }

                        val file = File(
                            FileManager.getInstance().apkFolder,
                            DownloadWorker.UPDATE_FILE_NAME
                        )
                        if (file.exists() && file.isFile && !TextUtils.isEmpty(md5)) {
                            if (TextUtils.equals(md5, FileUtils.getFileMD5(file))) {
                                showInstallDialog(file.path)
                                onUpgradeListener?.onUpgrade(true)
                                return@withContext
                            }
                        }

                        if (!TextUtils.isEmpty(url)) {
                            if (autoUpgrade && NetworkUtils.isWifiConnected()) {
                                downApk(url, md5, true)
                            } else {
                                showUpgradeDialog(data)
                            }
                        }
                    }
                    onUpgradeListener?.onUpgrade(data.isNeedUpdate())
                }

            } catch (e: Throwable) {
                // 请求失败
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onUpgradeListener?.onUpgrade(false)
                    mProgressDialog?.dismiss()
                }
            } finally {
                // 必须执行
                isRequesting = false
                withContext(Dispatchers.Main) {
                    mProgressDialog?.dismiss()
                }
            }
        }
    }


    private fun downApk(url: String?, md5: String?, autoUpdate: Boolean) {
        context.requestRxPermissions(
            delayText = "升级新版本需要访问手机存储，请授予SD卡权限",
            permissions = PermissionsUtils.getAndroid12WritePermission()
        ) { aBoolean ->
            if (aBoolean) {
                if (autoUpdate) {
                    ("当前为WIFI环境,将自动为你下载更新" + Session.getInstance().appName + "。").showToast()
                }
                DownloadWorker.enqueue(context, url, md5, null, autoUpdate, null)
            } else {
                dialog?.setDownloading(false)
            }
        }

    }

    private fun showUpgradeDialog() {
        mProgressDialog = ProgressDialog(context).apply {
            setMessage("正在检查新版本")
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            show()
        }
    }

    private fun showUpgradeDialog(upgradeData: UpgradeData) {
        if (dialog != null && dialog!!.isShowing) {
            return
        }
        dialog = UpgradeDialogFragment.show(
            context.supportFragmentManager,
            upgradeData.targetVersion,
            upgradeData.releaseNote,
            upgradeData.isNeedForceUpdate()
        )
        dialog?.onUpgradeClick = {
            downApk(upgradeData.upgradeUrl, upgradeData.md5Hash, false)
            null
        }
        dialog?.onRemindLater = {
            Config.get().markRecommendUpgradeDismissed()
        }
    }

    // 替代原 @Subscribe 方法
    private fun onFinishDownload(downloadApkEvent: DownloadWorker.DownloadApkEvent) {
        showInstallDialog(downloadApkEvent.path)
    }

    private fun showInstallDialog(path: String) {
        if (dialog != null && dialog!!.isShowing) {
            return
        }
        val data = upgradeData ?: return

        dialog = UpgradeDialogFragment.show(
            context.supportFragmentManager,
            data.targetVersion,
            data.releaseNote,
            data.isNeedForceUpdate()
        )
        dialog?.updateInstall(path)
        dialog?.onRemindLater = {
            Config.get().markRecommendUpgradeDismissed()
        }

    }

    fun release() {
        onUpgradeListener = null
        // EventBus.getDefault().unregister(this) // 不需要了，协程会自动取消
        mProgressDialog?.dismiss()
        mProgressDialog = null
    }

    companion object {
        private const val RECOMMEND_UPGRADE_REMIND_INTERVAL_MILLIS = 60L * 60 * 24 * 1000
    }
}
