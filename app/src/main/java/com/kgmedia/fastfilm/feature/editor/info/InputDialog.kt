package com.kgmedia.fastfilm.feature.editor.info

import android.content.DialogInterface
import android.view.KeyEvent
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ThreadUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.applySmartFocusManager
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.databinding.DialogInputBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.showToast

/**
 * 修改描述-通用版
 */
class InputDialog :
    BaseBottomPushVMDialogFragment<DialogInputBinding, InputViewModel>() {

    companion object {
        const val TYPE_WORK_DESC = 0
        const val TYPE_EPISODE_SUMMARY = 1
        fun show(
            type: Int? = null,
            workId: String? = null,
            storyId: String? = null,
            content: String? = null,
            extra: String? = null,
            callback: (String) -> Unit
        ) {
            val activity = topActivity as? FragmentActivity ?: return
            InputDialog().apply {
                arguments = bundleOf(
                    Constants.Extra.WORK_ID to workId,
                    Constants.Extra.STORY_ID to storyId,
                    Constants.Extra.TYPE to type,
                    Constants.Extra.CONTENT to content,
                    Constants.Extra.EXTRA to extra
                )
                this.callback = callback
            }.show(activity.supportFragmentManager)
        }
    }

    var callback: ((String) -> Unit)? = null


    override fun onStart() {
        super.onStart()

        // 2. 拦截物理返回键
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                closeConfirm() // 统一走你的检查逻辑
                true // 表示我们已经消费了该事件
            } else {
                false
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        binding.inputView.setTextNoSelection("")
        binding.close.setOnClickListener {
            closeConfirm()
        }

        mRealVM.input.observe(this) {
            if (it != binding.inputView.text) {
                mRealVM.hasChange = true
                binding.inputView.setTextNoSelection(it)
            }
        }

        binding.btnOk.setOnClickListener {
            if (binding.inputView.text.trim().isEmpty()) {
                getString(R.string.enter_update_content).showToast()
                return@setOnClickListener
            }
            if (mRealVM.content != binding.inputView.text.trim()) {
                mRealVM.save {
                    callback?.invoke(binding.inputView.text.trim())
                    dismissAllowingStateLoss()
                }
            } else {
                dismissAllowingStateLoss()
            }
        }
    }

    fun closeConfirm() {
        if (mRealVM.hasChange && mRealVM.content != binding.inputView.text.trim()) {
            topActivity?.showConfirmPrompt(
                title = getString(R.string.confirm_exit_without_saving),
                cancelText = getString(R.string.exit_directly),
                confirmText = getString(R.string.save_and_exit),
                cancel = {
                    dismissAllowingStateLoss()
                },
                confirm = {
                    mRealVM.save {
                        callback?.invoke(binding.inputView.text.trim())
                        dismissAllowingStateLoss()
                    }
                })
        } else {
            dismissAllowingStateLoss()
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