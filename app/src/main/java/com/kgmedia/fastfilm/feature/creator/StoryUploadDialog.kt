package com.kgmedia.fastfilm.feature.creator

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.os.Build
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.getFileMeta
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.permissions.requestRead2WritePermissions
import com.kgmedia.fastfilm.common.utils.FileCountUtils
import com.kgmedia.fastfilm.common.utils.TextEncodingUtil
import com.kgmedia.fastfilm.databinding.DialogStoryUploadBinding
import com.kgmedia.fastfilm.feature.creator.StoryUploadViewModel.Companion.STATE_UPLOAD_FAIL
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 微信客服二维码
 */
class StoryUploadDialog : BaseVMDialogFragment<DialogStoryUploadBinding, StoryUploadViewModel>() {

    companion object {
        fun show(
            fileName: String?,
            fileUrl: String?,
            result: FileCountUtils.AnalysisResult?,
            callback: (String?, String?, FileCountUtils.AnalysisResult?) -> Unit
        ) {
            val activity = topActivity as? FragmentActivity ?: return
            StoryUploadDialog().apply {
                arguments = bundleOf(Constants.Extra.NAME to fileName, Constants.Extra.URL to fileUrl, Constants.Extra.RESULT to result)
                this.uploadCallback = callback
            }.show(activity.supportFragmentManager)
        }
    }

    var uploadCallback: (String?, String?, FileCountUtils.AnalysisResult?) -> Unit = { _, _, _ -> }
    var analyzeResult: FileCountUtils.AnalysisResult? = null

    override fun onPrepareView() {
        super.onPrepareView()
        analyzeResult = arguments?.getParcelableValue(Constants.Extra.RESULT, FileCountUtils.AnalysisResult::class.java)
        binding.btnChoose.paintFlags = binding.btnChoose.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.btnChoose.setOnSingleClickListener {
            openFilePicker()
        }
        binding.cancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.confirm.setOnSingleClickListener {
            mRealVM.powerCalculate(analyzeResult?.charCount){
                analyzeResult?.estimatedPower = it?.toDouble() ?: 0.0
                uploadCallback?.invoke(mRealVM.fileName.get(), mRealVM.fileUrl, analyzeResult)
                dismissAllowingStateLoss()
            }
        }
    }

    /**
     * 申请权限并保存图片
     */
    private fun requestReadPermission(callback: () -> Unit) {
        requestRead2WritePermissions { granted ->
            if (granted) {
                //权限申请成功
                callback.invoke()
            } else {
                //权限申请失败
            }
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnClose -> dismissAllowingStateLoss()
        }
    }

    // 1. 定义 Launcher
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                // 在协程中处理，防止卡顿
                lifecycleScope.launch(Dispatchers.IO) {
                    analyzeResult = FileCountUtils.getFileAnalysis(context, it)

                    it.getFileMeta(context).let { meta ->
                        mRealVM.fileName.set(meta.first + "." + meta.second)
                        val allowedExtensions = listOf("txt")//, "doc", "docx"
                        if (meta.second !in allowedExtensions) {
                            withContext(Dispatchers.Main) {
                                "文件格式不支持，请选择 TXT 文件".showToast()
                            }
                            return@launch
                        }
                    }
                    val pathFromUri = TextEncodingUtil.createUtf8TempFile(mActivity, uri)?.absolutePath
//                    val pathFromUri = uri.getTempFilePathFromUri(mActivity)
                    withContext(Dispatchers.Main) {
                        if ((analyzeResult?.charCount ?: 0) > FileCountUtils.MAX_CHAR_COUNT) {
                            mRealVM.uploadErrorType.set(StoryUploadViewModel.UPLOAD_ERROR_TYPE_FILE_TOO_LARGE)
                            mRealVM.uploadStatus.set(STATE_UPLOAD_FAIL)
                        } else {
                            mRealVM.startUpload(pathFromUri)
                        }
                    }
                }
            }
        }
    }

    // 2. 触发选择（包含权限检查）
    fun openFilePicker() {
        val pickerLauncher = {
            //, "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain" // 设置一个基础类型
                val mimeTypes = arrayOf(
                    "text/plain",
                )
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            filePickerLauncher.launch(intent)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            requestReadPermission {
                pickerLauncher.invoke()
            }
        } else {
            pickerLauncher.invoke()
        }
    }
}