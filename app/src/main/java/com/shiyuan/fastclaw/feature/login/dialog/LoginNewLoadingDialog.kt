package com.shiyuan.fastclaw.feature.login.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import com.nhsd.basic.BaseDialogFragment
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogPhoneNewLoginLoadingBinding

class LoginNewLoadingDialog : BaseDialogFragment<DialogPhoneNewLoginLoadingBinding>() {

    companion object {
        const val TAG = "PhoneNewLoginLoadingDialog"
        private const val ARG_TEXT = "text"

        /**
         * 创建新版登录 Loading 弹框，可复用不同业务文案。
         */
        fun newInstance(text: String? = null): LoginNewLoadingDialog {
            return LoginNewLoadingDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, text)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), R.style.LoadingDialog).apply {
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvLoadingText.text = arguments?.getString(ARG_TEXT) ?: getString(R.string.login_new_logging_in)
    }

    /**
     * 按登录中设计稿设置弹窗尺寸和位置。
     */
    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = dp(139)
                attr.height = dp(126)
                attr.gravity = Gravity.CENTER
                attr.y = -dp(18)
                dialogWindow.attributes = attr
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }
}
