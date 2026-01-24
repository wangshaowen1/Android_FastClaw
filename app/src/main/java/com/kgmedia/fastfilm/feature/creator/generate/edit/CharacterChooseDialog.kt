package com.kgmedia.fastfilm.feature.creator.generate.edit

import android.os.Bundle
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.network.apis.response.Role
import com.kgmedia.fastfilm.databinding.DialogCharacterChooseBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 角色选择
 */
class CharacterChooseDialog : BaseBottomPushVMDialogFragment<DialogCharacterChooseBinding, CharacterChooseViewModel>() {

    companion object {
        fun newInstance(roles: List<Role>?): CharacterChooseDialog {
            val fragment = CharacterChooseDialog()
            val bundle = Bundle().apply {
                roles?.let {
                    putParcelableArrayList(Constants.Extra.CONTENT, ArrayList(roles))
                }
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    var onSuccess: (Role) -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()

        mRealVM.onRoleChoose = {
            onSuccess.invoke(it)
            dismissAllowingStateLoss()
        }
    }
}