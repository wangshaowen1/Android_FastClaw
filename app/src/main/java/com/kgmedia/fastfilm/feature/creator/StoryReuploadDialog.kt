package com.kgmedia.fastfilm.feature.creator

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.common.utils.FileCountUtils
import com.kgmedia.fastfilm.databinding.DialogStoryReuploadBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 小说文档检测到敏感词
 */
class StoryReuploadDialog : BaseVMDialogFragment<DialogStoryReuploadBinding, StoryUploadViewModel>() {

    companion object {
        fun show(
            fileName: String?,
            fileUrl: String?,
            result: FileCountUtils.AnalysisResult?,
            callback: (String?, String?, FileCountUtils.AnalysisResult?) -> Unit
        ) {
            val activity = topActivity as? FragmentActivity ?: return
            StoryReuploadDialog().apply {
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

        binding.confirm.setOnSingleClickListener {
            uploadCallback?.invoke(mRealVM.fileName.get(), mRealVM.fileUrl, analyzeResult)
            dismissAllowingStateLoss()
        }
    }
}