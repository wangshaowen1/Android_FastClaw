package com.kgmedia.fastfilm.feature.settings.personal

import android.content.DialogInterface
import android.graphics.Typeface
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ThreadUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.databinding.DialogEditIntroductionBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import java.lang.ref.WeakReference

/**
 * 修改简介
 */
class EditIntroductionDialog(private val useSharedViewModel: Boolean=true) : BaseBottomPushVMDialogFragment<DialogEditIntroductionBinding, PersonalInformationViewModel>(useSharedViewModel) {

    private val showInput
        get() = object : Runnable {
            val edit by lazy { WeakReference(binding.etIntroduction) }
            override fun run() {
                edit.get()?.let {
                    KeyboardUtils.showSoftInput(it)
                    it.requestFocus()
                    it.setSelection(it.length())
                }
            }
        }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvTitle.typeface = Typeface.DEFAULT_BOLD
    }

    override fun onResume() {
        super.onResume()
        ThreadUtils.runOnUiThreadDelayed(showInput, 200)
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.tv_close -> {
                dismissAllowingStateLoss()
            }

            R.id.btnOk -> {
                mRealVM.commit(signature = mRealVM.editSignature.value?.trim()) {
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

    override fun onDismiss(dialog: DialogInterface) {
        hideSoftInput()
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        hideSoftInput()
        super.onDestroy()
    }

    private fun hideSoftInput() {
        ThreadUtils.runOnUiThreadDelayed({ KeyboardUtils.hideSoftInput(ActivityUtils.getTopActivity()) }, 100)
    }

    override fun transparentNavBar() {}
}