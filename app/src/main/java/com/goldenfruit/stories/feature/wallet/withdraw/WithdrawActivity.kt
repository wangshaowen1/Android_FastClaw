package com.kgmedia.fastfilm.feature.wallet.withdraw

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.ext.formatMoney
import com.kgmedia.fastfilm.databinding.ActivityWithdrawBinding
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import kotlin.math.min

/**
 * 收益提现
 */
class WithdrawActivity : BaseViewModelActivity<ActivityWithdrawBinding, WithdrawViewModel>() {
    override fun setTranslucentStatus() {
        ImmersionBar.with(this).statusBarDarkFont(true).navigationBarColor(R.color.color_f6f6f6).init()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()
                if (text.isNullOrEmpty()) {
                    mRealVM.onInput(text)
                    return
                }
                val num = text.toFloatOrNull() ?: 0f
                val max = min(mRealVM.summary.get()?.maxWithdrawalAmount ?: 0f, mRealVM.summary.get()?.balance ?: 0f)
                if (num > max) {
                    "当前最多可提现${max.formatMoney()}元".showToast()
                    delete()
                    return
                }
                if (text.startsWith(".")) {
                    binding.etAmount.setText("")
                    return
                }
                val xs = text.split(".").getOrNull(1) ?: ""
                if (xs.length > 2) {
                    delete()
                    return
                }
                mRealVM.onInput(text)
            }
        })
    }

    private fun delete() {
        val length = binding.etAmount.length()
        binding.etAmount.text?.delete(length - 1, length)
    }

    companion object {
        fun start(context: Context?) {
            val intent = Intent(context, WithdrawActivity::class.java)
            context?.startActivity(intent)
        }
    }
}