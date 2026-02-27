package com.kgmedia.fastfilm.feature.creator.set

import androidx.core.os.bundleOf
import com.kgmedia.fastfilm.databinding.DialogStoryGenerateConfirmBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment


class StoryGenerateConfirmDialog : BaseBottomPushVMDialogFragment<DialogStoryGenerateConfirmBinding, StoryGenerateConfirmViewModel>() {

    companion object {
        fun newInstance(): StoryGenerateConfirmDialog {
            val fragment = StoryGenerateConfirmDialog()
            fragment.arguments = bundleOf(

            )
            return fragment
        }
    }

    var onConfirm: () -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        binding.btnConfirm.setOnClickListener {
            onConfirm.invoke()
            dismissAllowingStateLoss()
        }
    }


}