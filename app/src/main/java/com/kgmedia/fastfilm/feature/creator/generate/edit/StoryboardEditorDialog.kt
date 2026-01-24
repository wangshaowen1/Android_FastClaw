package com.kgmedia.fastfilm.feature.creator.generate.edit

import android.os.Bundle
import android.text.Editable
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.SimpleTextWatcher
import com.kgmedia.fastfilm.core.network.apis.response.ShotSegmentInfo
import com.kgmedia.fastfilm.databinding.DialogStoryboardEditorBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.showToast

/**
 * 故事内容编辑
 */
class StoryboardEditorDialog : BaseBottomPushVMDialogFragment<DialogStoryboardEditorBinding, StoryboardEditorViewModel>() {

    companion object {
        const val TAG = "StoryboardEditorDialog"

        fun newInstance(dramaId: String?, segment: ShotSegmentInfo?, onSuccess: (String?) -> Unit): StoryboardEditorDialog {
            val fragment = StoryboardEditorDialog()
            val bundle = Bundle()
            bundle.putParcelable(Constants.Extra.CONTENT, segment)
            dramaId?.let { bundle.putString(Constants.Extra.ID, dramaId) }
            fragment.arguments = bundle
            fragment.onSuccess = onSuccess
            return fragment
        }
    }

    var onSuccess: (String?) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        mRealVM.input.observe(this) {
            if (!it.isNullOrEmpty() && binding.inputer.text.isNullOrEmpty()) {
                binding.inputer.setText(it)
            }
        }
        binding.inputer.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                binding.btnConfirm.alpha = if (text.isEmpty()) 0.5f else 1f
                mRealVM.input.value = text
            }
        })
        binding.btnAddRole.setOnClickListener {
//                CharacterChooseDialog.newInstance(it).apply {
//                    onSuccess = { role ->
//                        binding.inputer.insertRole(role)
//                    }
//                }.show(childFragmentManager)
        }
        binding.btnConfirm.setOnClickListener {
            if (binding.btnConfirm.alpha < 1f) {
                "请输入内容".showToast()
                return@setOnClickListener
            }
            mRealVM.modifyContent(binding.inputer.toServerFormat()) {
                onSuccess.invoke(it)
                dismissAllowingStateLoss()
            }
        }
    }
}