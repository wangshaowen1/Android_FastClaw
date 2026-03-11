package com.kgmedia.fastfilm.feature.editor.info

import android.content.DialogInterface
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ThreadUtils
import com.kgmedia.fastfilm.common.ext.applySmartFocusManager
import com.kgmedia.fastfilm.databinding.DialogDramaDescEditBinding
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.showToast

/**
 * 修改简介
 */
class DramaDescEditDialog() :
    BaseBottomPushDialogFragment<DialogDramaDescEditBinding>() {

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        binding.inputView.setTextNoSelection("")
        binding.close.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.btnOk.setOnClickListener {
            if (binding.inputView.text.trim().isEmpty()) {
                "请输入需要更新的内容".showToast()
                return@setOnClickListener
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
}