package com.shiyuan.fastclaw.feature.resume.experience

import android.view.Gravity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.DialogDeleteConfirmBinding

/**
 * @Introduce: 删除二次确认弹窗
 */
class DeleteConfirmDialog : BaseDialogFragment<DialogDeleteConfirmBinding>() {

    companion object {
        private const val KEY_CONTENT = "content"
        private const val KEY_CANCEL_TEXT = "cancelText"
        private const val KEY_DELETE_TEXT = "deleteText"

        private const val DEFAULT_CONTENT = "删除后无法恢复，确定要删除吗?"
        private const val DEFAULT_CANCEL_TEXT = "取消"
        private const val DEFAULT_DELETE_TEXT = "删除"

        fun show(
            manager: FragmentManager,
            content: String = DEFAULT_CONTENT,
            cancelText: String = DEFAULT_CANCEL_TEXT,
            deleteText: String = DEFAULT_DELETE_TEXT,
            onCancel: (() -> Unit)? = null,
            onDelete: (() -> Unit)? = null,
        ): DeleteConfirmDialog {
            return DeleteConfirmDialog().apply {
                arguments = bundleOf(
                    KEY_CONTENT to content,
                    KEY_CANCEL_TEXT to cancelText,
                    KEY_DELETE_TEXT to deleteText
                )
                this.onCancel = onCancel
                this.onDelete = onDelete
                show(manager)
            }
        }
    }

    var onCancel: (() -> Unit)? = null
    var onDelete: (() -> Unit)? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        bindContent()
        bindClick()
    }

    private fun bindContent() {
        val args = arguments
        binding.tvContent.text = args?.getString(KEY_CONTENT, DEFAULT_CONTENT) ?: DEFAULT_CONTENT
        binding.tvCancel.text = args?.getString(KEY_CANCEL_TEXT, DEFAULT_CANCEL_TEXT) ?: DEFAULT_CANCEL_TEXT
        binding.tvDelete.text = args?.getString(KEY_DELETE_TEXT, DEFAULT_DELETE_TEXT) ?: DEFAULT_DELETE_TEXT
    }

    private fun bindClick() {
        binding.btnCancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
            onCancel?.invoke()
        }
        binding.btnDelete.setOnSingleClickListener {
            dismissAllowingStateLoss()
            onDelete?.invoke()
        }
    }
}
