package com.kgmedia.fastfilm.feature.creator.generate.edit

import android.text.Editable
import androidx.core.os.bundleOf
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.SimpleTextWatcher
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateBody
import com.kgmedia.fastfilm.databinding.DialogStoryOutlineEditorBinding
import com.kgmedia.fastfilm.feature.creator.generate.StoryEditorViewModel
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.showToast

/**
 * 故事剧本编辑
 */
class StoryOutlineEditorDialog : BaseBottomPushVMDialogFragment<DialogStoryOutlineEditorBinding, StoryOutlineEditorViewModel>() {

    companion object {
        fun newInstance(dramaId: String?, content: String?): StoryOutlineEditorDialog {
            val fragment = StoryOutlineEditorDialog()
            fragment.arguments = bundleOf(Constants.Extra.CONTENT to content, Constants.Extra.ID to dramaId)
            return fragment
        }
    }

    var onSuccess: (String?) -> Unit = {}
    val hostVm by lazy { getViewModelOfActivity<StoryEditorViewModel>() }

    override fun onPrepareView() {
        super.onPrepareView()
        val isCustomizeModel = hostVm?.model == DramaGenerateBody.GEN_WAY_CUSTOMIZED || hostVm?.model == DramaGenerateBody.GEN_WAY_CONTINUE_STORY
        Config.get().appConfig.shortDramas?.let {
            mRealVM.maxInputLength.set(
                if (isCustomizeModel) it.shortDramasWriteStoryLimitNumForVip ?: 5000 else it.shortDramasEditStoryLimitNum ?: 2000
            )
        }
        mRealVM.input.observe(this) {
            if (mRealVM.input.value != binding.inputer.text) {
                binding.inputer.setTextNoSelection(mRealVM.input.value)
            }
        }
//        binding.inputer.setOnTextOutOfBondsListener {
//            mRealVM.maxInputLength.takeIf { it > 0 }.let {
//                getString(R.string.toast_input_max_characters_exceeded, it).showToast()
//            }
//        }
        binding.inputer.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                binding.btnConfirm.alpha = if (text == mRealVM.content) 0.5f else 1f
                mRealVM.input.value = text
            }
        })
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.btnConfirm.setOnClickListener {
            if (binding.btnConfirm.alpha < 1f || mRealVM.input.value?.trim().isNullOrEmpty()) {
                "请输入需要更新的内容".showToast()
                return@setOnClickListener
            }
            mRealVM.modifyContent {
                onSuccess.invoke(mRealVM.input.value)
                dismissAllowingStateLoss()
            }
        }
    }
}