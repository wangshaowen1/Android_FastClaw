package com.shiyuan.fastclaw.widget

import android.animation.ObjectAnimator
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.animation.LinearInterpolator
import androidx.fragment.app.FragmentManager
import com.nhsd.basic.BaseDialogFragment
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.DialogCommonLoadingBinding

class CommonLoadingDialog : BaseDialogFragment<DialogCommonLoadingBinding>() {

    companion object {
        const val TAG = "CommonLoadingDialog"
        private const val ARG_TEXT = "text"

        fun newInstance(text: CharSequence? = null): CommonLoadingDialog {
            return CommonLoadingDialog().apply {
                arguments = Bundle().apply {
                    putCharSequence(ARG_TEXT, text)
                }
            }
        }
        fun show(manager: FragmentManager, text: CharSequence? = null) {
            if (manager.isStateSaved) return
            val dialog = manager.findFragmentByTag(TAG) as? CommonLoadingDialog
                ?: newInstance(text)
            if (!dialog.isAdded) {
                dialog.showNow(manager, TAG)
            }
        }

        fun hide(manager: FragmentManager) {
            (manager.findFragmentByTag(TAG) as? CommonLoadingDialog)
                ?.dismissAllowingStateLoss()
        }
    }

    private var rotationAnimator: ObjectAnimator? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), R.style.LoadingDialog).apply {
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvLoadingText.text = arguments?.getCharSequence(ARG_TEXT)
            ?: getString(R.string.common_loading)
    }

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
        startRotationAnimation()
    }

    override fun onStop() {
        stopRotationAnimation()
        super.onStop()
    }

    private fun startRotationAnimation() {
        if (rotationAnimator?.isStarted == true) return
        rotationAnimator = ObjectAnimator.ofFloat(binding.ivLoading, "rotation", 0f, 360f).apply {
            duration = 1000L
            interpolator = LinearInterpolator()
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            start()
        }
    }

    private fun stopRotationAnimation() {
        rotationAnimator?.cancel()
        rotationAnimator = null
        binding.ivLoading.rotation = 0f
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }
}
