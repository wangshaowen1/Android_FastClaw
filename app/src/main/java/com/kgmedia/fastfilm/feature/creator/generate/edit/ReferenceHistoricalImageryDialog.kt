package com.kgmedia.fastfilm.feature.creator.generate.edit

import androidx.core.os.bundleOf
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.network.apis.entity.ReferenceImageInfo
import com.kgmedia.fastfilm.databinding.DialogReferenceHistoricalImageryBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment


class ReferenceHistoricalImageryDialog :
    BaseBottomPushVMDialogFragment<DialogReferenceHistoricalImageryBinding, ReferenceHistoricalImageryViewModel>() {

    companion object {
        fun newInstance(lastChooseStyleId: String? = null): ReferenceHistoricalImageryDialog {
            val fragment = ReferenceHistoricalImageryDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.ID to lastChooseStyleId
            )
            return fragment
        }
    }

    var onSuccess: (ReferenceImageInfo?) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        binding.close.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnCancel.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnConfirm.setOnClickListener {
            onSuccess.invoke(mRealVM.checkedVm?.data?.get())
            dismissAllowingStateLoss()
        }
    }
}