package com.kgmedia.fastfilm.feature.editor.regenerate

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentInfo
import com.kgmedia.fastfilm.databinding.DialogStoryboardRegenerateBinding
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorViewModel
import com.kgmedia.fastfilm.widget.GeneratingDialog
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getParentViewModel

/**
 * 重绘分镜
 */
class StoryboardRegenerateDialog : BaseBottomPushVMDialogFragment<DialogStoryboardRegenerateBinding, StoryboardRegenerateViewModel>() {
    companion object {
        fun show(fragmentManager: FragmentManager?, dramasId: String?, segment: ShortDramaSegmentInfo?) {
            val fragmentManager = fragmentManager ?: (topActivity as? FragmentActivity)?.supportFragmentManager ?: return
            StoryboardRegenerateDialog().apply {
                arguments = Bundle().apply {
                    putString(Constants.Extra.DRAMAS_ID, dramasId)
                    putParcelable(Constants.Extra.DATA, segment)
                }
            }.show(fragmentManager)
        }
    }

    var dialog: GeneratingDialog? = null

    override fun onPrepareView() {
        super.onPrepareView()
        mRealVM.parentVM = getParentViewModel<ShortDramaEditorViewModel>()
        mRealVM.promptGenerating.observe(this) {
            if (it.first) {
                dialog?.dismiss()
                dialog = activity?.let { it1 -> GeneratingDialog(it1, it.second) }
                dialog?.show()
            } else {
                dialog?.dismiss()
            }
        }
    }
}