package com.kgmedia.fastfilm.feature.timbre

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.databinding.DialogGenerateWaitingBinding
import com.kgmedia.fastfilm.feature.aicompose.generating.GeneratingViewModel
import com.nhsd.basic.BaseBottomPushVMDialogFragment

class GenerateWaitingDialog : BaseBottomPushVMDialogFragment<DialogGenerateWaitingBinding, GenerateWaitingViewModel>() {

    companion object {

        const val TAG = "GeneratingActivity"
        const val BODY = "body"
        const val TYPE_PICTURE_BOOK_GENERATE = 1

        @JvmStatic
        fun show(fm: FragmentManager?, userPictureBookId: String?): GenerateWaitingDialog? {
            fm ?: return null
            val bundle = Bundle()
            bundle.putString(Constants.Extra.DRAMA_ID, userPictureBookId)
            bundle.putInt(Constants.Extra.TYPE, TYPE_PICTURE_BOOK_GENERATE)
            val dialog = GenerateWaitingDialog()
            dialog.arguments = bundle
            dialog.show(fm)
            return dialog
        }
    }

    val generatingVM get() = GeneratingViewModel

    override fun onPrepareView() {
        super.onPrepareView()
        generatingVM.generating = true

        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }
    }
}