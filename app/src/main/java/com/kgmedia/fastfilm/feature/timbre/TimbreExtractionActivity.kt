package com.kgmedia.fastfilm.feature.timbre

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.permissions.PermissionsUtils
import com.kgmedia.fastfilm.common.permissions.requestRxPermissions
import com.kgmedia.fastfilm.databinding.ActivityTimbreExtractionBinding
import com.kgmedia.fastfilm.kit.media.recorder.NoiseSuppressionHelper.processPcmFileWithNoiseSuppression
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 人声提取
 */
class TimbreExtractionActivity : BaseViewModelActivity<ActivityTimbreExtractionBinding, TimbreExtractionViewModel>() {

    companion object {
        const val EXTRA_TIMBRE_ID = "timbreId"

        @JvmStatic
        fun start(context: Context?, timbreId: String? = null) {
            if (context == null) return
            context.startActivity(Intent(context, TimbreExtractionActivity::class.java).apply {
                putExtra(EXTRA_TIMBRE_ID, timbreId)
            })
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initListener()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onBackPressed() {
        if (mRealVM.audioPath.get().isNullOrEmpty()) {
            super.onBackPressed()
        } else {
            showConfirmPrompt(tip = "离生成就差一步啦，确认放弃？", cancelText = "暂不", confirmText = "确认") {
                super.onBackPressed()
            }
        }
    }

    override fun onPause() {
        if (mRealVM.recording.get() && !mRealVM.recordPause.get()) {
            mRealVM.pauseRecord()
        }
        super.onPause()
    }

    private fun initListener() {
        binding.btnStart.setOnSingleClickListener {
            requestPermission {
                mRealVM.startRecord()
            }
        }
        binding.btnReRecord.setOnClickListener {
            showConfirmPrompt("确认重录吗？") {
                mRealVM.stopRecord()
                mRealVM.startRecord()
            }
        }
        binding.recordProgress.setOnClickListener {
            if (mRealVM.recording.get()) {
                if (mRealVM.recordPause.get()) {
                    mRealVM.resumeRecord()
                } else {
                    mRealVM.pauseRecord()
                }
            } else {
                "录制已完成，你可以点击“完成”提交或者点击“重录”".showToast()
            }
        }
        binding.btnCommit.setOnSingleClickListener {
            if (isShowConfirm) return@setOnSingleClickListener
            isShowConfirm = true
            mRealVM.stopRecord()
            lifecycleScope.launchNoCrash {
                showLoadingDialog()
                val outWavPath = withContext(Dispatchers.IO) {
                    processPcmFileWithNoiseSuppression(mRealVM.audioPath.get())
                }
                val path = if (outWavPath.isNullOrEmpty()) {
                    mRealVM.audioPath.get()
                } else {
                    outWavPath
                }
                hideLoadingDialog()
                val newInstance = CreateCloneTimbreDialog.newInstance(path, mRealVM.recordDuring)
                newInstance.onConfirmClick = {
                    mRealVM.timbreCloneGenerate(path) { id ->
                        setResult(id)
                    }
                }
                newInstance.setOnDismissListener { isShowConfirm = false }
                newInstance.show(supportFragmentManager)
            }
        }
    }

    var isShowConfirm = false

    fun setResult(id: String?) {
        setResult(RESULT_OK, Intent().putExtra(Constants.Extra.ID, id))
        Logger.d("MyTimbresActivity", "onStart: id = ${id}")
        finish()
    }

    private fun requestPermission(onGranted: () -> Unit) {
        val audioPermission = PermissionsUtils.getRecordAudioPermission()
        requestRxPermissions(
            R.string.sdcard_permission_description,
            *audioPermission
        ) { granted ->
            if (granted) {
                onGranted.invoke()
            }
        }
    }

}