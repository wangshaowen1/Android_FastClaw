package com.kgmedia.fastfilm.feature.creator.set

import androidx.core.os.bundleOf
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.utils.FileCountUtils
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryData
import com.kgmedia.fastfilm.databinding.DialogStoryGenerateConfirmBinding
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getParcelableValue
import kotlin.math.roundToInt

class StoryGenerateConfirmDialog : BaseBottomPushVMDialogFragment<DialogStoryGenerateConfirmBinding, StoryGenerateConfirmViewModel>() {

    companion object {
        fun newInstance(fileUrl: String?, result: FileCountUtils.AnalysisResult?): StoryGenerateConfirmDialog {
            val fragment = StoryGenerateConfirmDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.URL to fileUrl,
                Constants.Extra.RESULT to result
            )
            return fragment
        }
    }

    var onConfirm: (UploadStoryData?) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        val result = arguments?.getParcelableValue(Constants.Extra.RESULT, FileCountUtils.AnalysisResult::class.java)
        val fileUrl = arguments?.getString(Constants.Extra.URL)
        binding.tvPower.text = result?.estimatedPower?.roundToInt().toString()
        binding.btnConfirm.setOnClickListener {
            mRealVM.confirmPowerConsume(fileUrl, result) { data ->
                if (data?.checkStatus() == true) {
                    onConfirm.invoke(data)
                    dismissAllowingStateLoss()
                } else {
                    PowerRechargeDialog.show()
                }
            }
        }
    }
}