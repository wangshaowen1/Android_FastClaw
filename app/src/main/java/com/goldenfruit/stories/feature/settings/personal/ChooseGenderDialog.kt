package com.kgmedia.fastfilm.feature.settings.personal

import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.databinding.DialogChooseGenderBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment

/**
 * 修改性别
 */
class ChooseGenderDialog(private val useSharedViewModel: Boolean=true) : BaseBottomPushVMDialogFragment<DialogChooseGenderBinding, PersonalInformationViewModel>(useSharedViewModel) {


    override fun onPrepareView() {
        super.onPrepareView()
        mRealVM.editGender.set(mRealVM.gender.get())
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnClose -> {
                dismissAllowingStateLoss()
            }

            R.id.genderFemale->{
                mRealVM.editGender.set(User.FEMALE)
            }
            R.id.genderMale->{
                mRealVM.editGender.set(User.MALE)
            }

            R.id.btnOk -> {
                mRealVM.commit(gender = mRealVM.editGender.get()) {
                    mRealVM.gender.set(mRealVM.editGender.get())
                    if (!useSharedViewModel){
                        dismissAllowingStateLoss()
                    }
                }
                if (useSharedViewModel){
                    dismissAllowingStateLoss()
                }
            }
        }
    }

}