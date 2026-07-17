package com.shiyuan.fastclaw.feature.upgrade

import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.EmptyViewModel
import com.shiyuan.fastclaw.common.utils.FileUtils
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.databinding.DialogUpgradeBinding
import com.shiyuan.fastclaw.service.DownloadWorker
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import java.io.File
import kotlin.math.min


class UpgradeDialogFragment : BaseVMDialogFragment<DialogUpgradeBinding, EmptyViewModel>() {


    var onUpgradeClick: (() -> Unit)? = null
    var onRemindLater: (() -> Unit)? = null
    val force by lazy { arguments?.getBoolean(FORCE_UPGRADE, false) }
    private var remindLaterNotified = false
    private var installPath: String? = null

    override fun onPrepareView() {
        super.onPrepareView()
        binding.version.text = arguments?.getString(VERSION_TEXT)
        binding.content.text = arguments?.getString(UPGRADE_DESCRIPTION)
        if (force == true) {
            dialog?.setCancelable(false)
        }
        binding.cancel.visibility = force?.let {
            if (it) View.GONE else View.VISIBLE
        } ?: View.GONE
        observeEvents {
            on<Events.DownloadProgressEvent> { event ->
                if (isAdded) {
                    if (event.percent < 100) {
                        setDownloading(true)
                    }
                    binding.upgrade.text = "${event.percent}%"
                    if (event.percent == 100) {
                        if (force == false) {
                            dismissAllowingStateLoss()
                        } else if (!TextUtils.isEmpty(event.path)) {
                            updateInstall(event.path)
                        }
                    }
                }
            }
            on<DownloadWorker.DownloadFailedEvent> {
                setDownloading(false)
            }
        }
        installPath?.let(::updateInstall)
    }

    fun setDownloading(downloading: Boolean) {
        if (!isAdded) return
        binding.upgrade.isEnabled = !downloading
        if (!downloading) {
            binding.upgrade.setText(R.string.update_now)
        }
    }

    fun updateInstall(path: String) {
        installPath = path
        if (isAdded) {
            binding.upgrade.text = "立即安装"
            binding.upgrade.isEnabled = true
            binding.upgrade.setOnClickListener {
                FileUtils.openFile(context, File(path))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                val screenWidth = min(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight())
                attr.width = (280 / 375f * screenWidth).toInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.upgrade -> {
                setDownloading(true)
                onUpgradeClick?.invoke()
            }

            R.id.cancel -> {
                notifyRemindLater()
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        notifyRemindLater()
        super.onCancel(dialog)
    }

    private fun notifyRemindLater() {
        if (force == false && !remindLaterNotified) {
            remindLaterNotified = true
            onRemindLater?.invoke()
        }
    }

    companion object {

        const val VERSION_TEXT: String = "versionText"
        const val UPGRADE_DESCRIPTION: String = "upgradeDescription"
        const val FORCE_UPGRADE: String = "forceUpgrade"

        fun show(manager: FragmentManager, versionText: String?, upgradeDescription: String?, forceUpgrade: Boolean): UpgradeDialogFragment {
            val dialog = UpgradeDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(VERSION_TEXT, versionText)
                    putString(UPGRADE_DESCRIPTION, upgradeDescription)
                    putBoolean(FORCE_UPGRADE, forceUpgrade)
                }
            }
            dialog.show(manager)
            return dialog
        }
    }
}
