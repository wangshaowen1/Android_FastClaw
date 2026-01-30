package com.kgmedia.fastfilm.feature.creator.set

import androidx.core.os.bundleOf
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.network.apis.entity.CreationStyleInfo
import com.kgmedia.fastfilm.core.network.apis.entity.VideoConfigData
import com.kgmedia.fastfilm.databinding.DialogVideoSetBinding
import com.kgmedia.fastfilm.feature.creator.style.CreationStyleChooseDialog
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast


class VideoSetDialog : BaseBottomPushVMDialogFragment<DialogVideoSetBinding, VideoSetViewModel>() {

    companion object {
        fun newInstance(list: VideoConfigData?): VideoSetDialog {
            val fragment = VideoSetDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.DATA to list
            )
            return fragment
        }
    }

    var onSuccess: (CreationStyleInfo?) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()

        binding.btnAllStyle.setOnSingleClickListener {
            val newInstance = CreationStyleChooseDialog.newInstance(mRealVM.checkedStyle?.data?.get()?.styleCode, mRealVM.configsData?.list)
            newInstance.onSuccess = {
                it?.let {
                    mRealVM.styleVms.find { vm -> vm.data.get()?.styleCode == it.styleCode }?.let { vm ->
                        vm.checked.set(true)
                        mRealVM.checkedStyle = vm
                    }
                }
            }
            newInstance.show(childFragmentManager)

            binding.btnConfirm.setOnClickListener {
                if (mRealVM.checkedEpisodeSet == null) {
                    "请设置故事总集数".showToast()
                    return@setOnClickListener
                }
                if (mRealVM.checkedRatio == null) {
                    "请选择视频尺寸".showToast()
                    return@setOnClickListener
                }
                if (mRealVM.checkedStyle == null) {
                    "请选择视频风格".showToast()
                    return@setOnClickListener
                }
//            onSuccess.invoke()
                dismissAllowingStateLoss()
            }
        }
    }
}