package com.kgmedia.fastfilm.feature.creator.set

import androidx.core.os.bundleOf
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.utils.TextEncodingUtil
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryBody
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryData
import com.kgmedia.fastfilm.databinding.DialogStoryGenerateConfirmBinding
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getParcelableValue
import kotlin.math.roundToInt

class StoryGenerateConfirmDialog : BaseBottomPushVMDialogFragment<DialogStoryGenerateConfirmBinding, StoryGenerateConfirmViewModel>() {

    companion object {
        const val SCENE_TXT_UPLOAD = 2
        fun newInstance(
            body: UploadStoryBody?,
            result: TextEncodingUtil.AnalysisResult?): StoryGenerateConfirmDialog {
            val fragment = StoryGenerateConfirmDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.DATA to body,
                Constants.Extra.RESULT to result,
            )
            return fragment
        }
    }

    var onConfirm: (UploadStoryData?) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        val result = arguments?.getParcelableValue(Constants.Extra.RESULT, TextEncodingUtil.AnalysisResult::class.java)
        binding.tvPower.text = result?.estimatedPower?.roundToInt().toString()
        binding.btnConfirm.setOnClickListener {
            mRealVM.confirmPowerConsume { data ->
                if (data?.checkStatus() == true) {
                    if (mRealVM.checkedPolicy.get()) {
                        Config.get().notifyPowerConsumeClose(SCENE_TXT_UPLOAD)
                    }
                    onConfirm.invoke(data)
                    dismissAllowingStateLoss()
                } else {
                    PowerRechargeDialog.show()
                }
            }
        }
    }
}