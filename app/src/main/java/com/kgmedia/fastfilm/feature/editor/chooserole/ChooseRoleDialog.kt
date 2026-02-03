package com.kgmedia.fastfilm.feature.editor.chooserole

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.DialogStoryboardChooseRoleBinding
import com.kgmedia.fastfilm.feature.editor.ShortDramaEditorViewModel
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getParentViewModel

/**
 * 重绘分镜-选择角色
 */
class ChooseRoleDialog : BaseBottomPushVMDialogFragment<DialogStoryboardChooseRoleBinding, ChooseRoleViewModel>() {
    companion object {
        fun show(fragmentManager: FragmentManager?, existRoleIds: ArrayList<String>) {
            val fragmentManager = fragmentManager ?: (topActivity as? FragmentActivity)?.supportFragmentManager ?: return
            ChooseRoleDialog().apply {
                arguments = Bundle().apply {
                    putStringArrayList(Constants.Extra.DATA, existRoleIds)
                }
            }.show(fragmentManager)
        }
    }

    private val existRoleIds by lazy { arguments?.getStringArrayList(Constants.Extra.DATA) }

    override fun onPrepareView() {
        super.onPrepareView()
    }

}