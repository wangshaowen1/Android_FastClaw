package com.kgmedia.fastfilm.feature.editor.history

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.DialogStoryboardHistoryBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 分镜历史
 */
class StoryboardHistoryDialog : BaseBottomPushVMDialogFragment<DialogStoryboardHistoryBinding, StoryboardHistoryViewModel>() {
    companion object {
        fun show(fragmentManager: FragmentManager?, dramasId: String?, shotSegmentId: String?, shotSegmentImageId: String?) {
            val fragmentManager = fragmentManager ?: (topActivity as? FragmentActivity)?.supportFragmentManager ?: return
            StoryboardHistoryDialog().apply {
                arguments = Bundle().apply {
                    putString(Constants.Extra.DRAMAS_ID, dramasId)
                    putString(Constants.Extra.ID, shotSegmentId)
                    putString(Constants.Extra.IMAGE_ID, shotSegmentImageId)
                }
            }.show(fragmentManager)
        }
    }
}