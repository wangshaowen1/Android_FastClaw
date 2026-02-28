package com.kgmedia.fastfilm.feature.creator.set

import androidx.core.os.bundleOf
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.utils.FileCountUtils
import com.kgmedia.fastfilm.databinding.DialogStoryGenerateConfirmBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getParcelableValue

class StoryGenerateConfirmDialog : BaseBottomPushVMDialogFragment<DialogStoryGenerateConfirmBinding, StoryGenerateConfirmViewModel>() {

    companion object {
        fun newInstance(result: FileCountUtils.AnalysisResult?): StoryGenerateConfirmDialog {
            val fragment = StoryGenerateConfirmDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.RESULT to result
            )
            return fragment
        }
    }

    var onConfirm: () -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        val result = arguments?.getParcelableValue(Constants.Extra.RESULT, FileCountUtils.AnalysisResult::class.java)
        binding.tvPower.text = result?.estimatedPower.toString()
        binding.btnConfirm.setOnClickListener {
            onConfirm.invoke()
            dismissAllowingStateLoss()
        }
    }


}