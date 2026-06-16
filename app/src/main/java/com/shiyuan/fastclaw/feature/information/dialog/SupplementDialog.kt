package com.shiyuan.fastclaw.feature.information.dialog

import android.view.Gravity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.DialogDeleteConfirmBinding

/**
 * @Introduce: 删除二次确认弹窗
 */
class SupplementDialog : BaseDialogFragment<DialogDeleteConfirmBinding>() {

    companion object {
        private const val KEY_CONTENT = "content"
        private const val KEY_LATER_TEXT = "laterText"
        private const val KEY_SUPPLEMENT_TEXT = "supplementText"

        private const val DEFAULT_CONTENT = "还有经历未补充完整"
        private const val DEFAULT_LATER_TEXT = "稍后填写"
        private const val DEFAULT_SUPPLEMENT_TEXT = "继续补充"

        fun show(
            manager: FragmentManager,
            content: String = DEFAULT_CONTENT,
            laterText: String = DEFAULT_LATER_TEXT,
            supplementText: String = DEFAULT_SUPPLEMENT_TEXT,
            onLater: (() -> Unit)? = null,
            onSupplement: (() -> Unit)? = null,
        ): SupplementDialog {
            return SupplementDialog().apply {
                arguments = bundleOf(
                    KEY_CONTENT to content,
                    KEY_LATER_TEXT to laterText,
                    KEY_SUPPLEMENT_TEXT to supplementText
                )
                this.onLater = onLater
                this.onSupplement = onSupplement
                show(manager)
            }
        }
    }

    var onLater: (() -> Unit)? = null
    var onSupplement: (() -> Unit)? = null

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
        binding.tvCancel.text = args?.getString(KEY_LATER_TEXT, KEY_LATER_TEXT) ?: KEY_LATER_TEXT
        binding.tvDelete.text = args?.getString(KEY_SUPPLEMENT_TEXT, KEY_SUPPLEMENT_TEXT) ?: KEY_SUPPLEMENT_TEXT
    }

    private fun bindClick() {
        binding.btnCancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
            onLater?.invoke()
        }
        binding.btnDelete.setOnSingleClickListener {
            dismissAllowingStateLoss()
            onSupplement?.invoke()
        }
    }
}
