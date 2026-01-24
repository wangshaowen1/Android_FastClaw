package com.kgmedia.fastfilm.feature.wallet.withdraw

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ScreenUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.network.apis.entity.WithdrawAccountInfo
import com.kgmedia.fastfilm.core.network.apis.entity.WithdrawConfirmInfo
import com.kgmedia.fastfilm.databinding.DialogWithdrawConfirmBinding
import com.nhsd.basic.BaseVMDialogFragment

/**
 * 提现确认
 */
class WithdrawConfirmDialog : BaseVMDialogFragment<DialogWithdrawConfirmBinding, WithdrawConfirmViewModel>() {
    companion object {
        fun show(amount: String, info: WithdrawConfirmInfo?, account: WithdrawAccountInfo, onWithdraw: () -> Unit) {
            val manager = (topActivity as? FragmentActivity)?.supportFragmentManager ?: return
            val dialog = WithdrawConfirmDialog()
            dialog.arguments = Bundle().apply {
                putString("amount", amount)
                putSerializable("info", info)
                putSerializable("account", account)
            }
            dialog.onWithdraw = onWithdraw
            dialog.show(manager)
        }
    }

    private var onWithdraw: (() -> Unit)? = null

    override fun onStart() {
        super.onStart()
        isCancelable = true
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = (287f / 375 * ScreenUtils.getScreenWidth()).toInt()
                dialogWindow.attributes = attr
            }
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnThink -> {
                dismissAllowingStateLoss()
            }

            R.id.btnConfirm -> {
                dismissAllowingStateLoss()
                onWithdraw?.invoke()
            }
        }
    }
}