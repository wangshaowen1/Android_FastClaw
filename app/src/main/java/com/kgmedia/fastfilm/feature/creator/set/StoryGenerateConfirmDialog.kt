package com.kgmedia.fastfilm.feature.creator.set

import androidx.core.os.bundleOf
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryBody
import com.kgmedia.fastfilm.core.network.apis.request.UploadStoryData
import com.kgmedia.fastfilm.databinding.DialogStoryGenerateConfirmBinding
import com.kgmedia.fastfilm.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import kotlin.math.roundToInt

class StoryGenerateConfirmDialog : BaseBottomPushVMDialogFragment<DialogStoryGenerateConfirmBinding, StoryGenerateConfirmViewModel>() {

    companion object {
        const val SCENE_TXT_UPLOAD = 2
        fun newInstance(
            body: UploadStoryBody?,
            powerConsume: Int?
        ): StoryGenerateConfirmDialog {
            val fragment = StoryGenerateConfirmDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.DATA to body,
                Constants.Extra.COST to powerConsume,
            )
            return fragment
        }
    }

    var onConfirm: (UploadStoryData?) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        val powerConsume = arguments?.getInt(Constants.Extra.COST)
        binding.tvPower.text = powerConsume?.toString()
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