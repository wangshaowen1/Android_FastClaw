package com.kgmedia.fastfilm.feature.play.more

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.databinding.DialogShortInfoEditorBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 修改视频信息
 */
class ShortInfoEditorDialog : BaseBottomPushVMDialogFragment<DialogShortInfoEditorBinding, ShortInfoEditorViewModel>(), TextWatcher {

    companion object {
        fun show(
            manager: FragmentManager,
            dramaId: String?,
            dramaName: String?,
            dramaIntro: String?
        ): ShortInfoEditorDialog {
            val dialog = ShortInfoEditorDialog()
            dialog.arguments = bundleOf(
                Constants.Extra.DRAMA_ID to dramaId,
                Constants.Extra.DRAMA_NAME to dramaName,
                Constants.Extra.DRAMA_INTRO to dramaIntro
            )
            dialog.show(manager)
            return dialog
        }
    }

    private val dramaId by lazy { arguments?.getString(Constants.Extra.DRAMA_ID) }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.storyTitleInput.addTextChangedListener(this)
        binding.storyIntroInput.addTextChangedListener(this)
        binding.storyTitleInput.setText(arguments?.getString(Constants.Extra.DRAMA_NAME))
        binding.storyIntroInput.setText(arguments?.getString(Constants.Extra.DRAMA_INTRO))
        updateConfirmButton()
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnOk -> {
                mRealVM.editShotInfo(
                    dramaId,
                    binding.storyTitleInput.text.toString(),
                    binding.storyIntroInput.text.toString()
                )
            }
        }
    }

    override fun beforeTextChanged(
        p0: CharSequence?,
        p1: Int,
        p2: Int,
        p3: Int
    ) {

    }

    override fun onTextChanged(
        p0: CharSequence?,
        p1: Int,
        p2: Int,
        p3: Int
    ) {
    }

    override fun afterTextChanged(p0: Editable?) {
        updateConfirmButton()
    }

    fun updateConfirmButton() {
        binding.btnOk.isEnabled =
            binding.storyTitleInput.text.isNotEmpty() && binding.storyIntroInput.text.isNotEmpty()

    }

}