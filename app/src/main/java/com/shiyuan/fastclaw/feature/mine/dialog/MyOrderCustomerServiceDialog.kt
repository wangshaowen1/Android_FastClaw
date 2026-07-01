package com.shiyuan.fastclaw.feature.mine.dialog

import android.view.Gravity
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.DialogMyOrderCustomerServiceBinding

class MyOrderCustomerServiceDialog :
    BaseDialogFragment<DialogMyOrderCustomerServiceBinding>() {

    companion object {
        fun show(
            manager: FragmentManager,
            onConfirm: (() -> Unit)? = null,
        ): MyOrderCustomerServiceDialog {
            return MyOrderCustomerServiceDialog().apply {
                this.onConfirm = onConfirm
                show(manager)
            }
        }
    }

    private var onConfirm: (() -> Unit)? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = (ScreenUtils.getScreenWidth() * 296f / 375f).toInt()
                attr.height = (ScreenUtils.getScreenWidth() * 168f / 375f).toInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.btnCancel.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
        binding.btnConfirm.setOnSingleClickListener {
            dismissAllowingStateLoss()
            onConfirm?.invoke()
        }
    }
}
