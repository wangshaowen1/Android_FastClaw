package com.kgmedia.fastfilm.feature.settings.personal

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ThreadUtils
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.databinding.DialogNickNameEditorBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import java.lang.ref.WeakReference

/**
 * 修改昵称
 */
class NicknameEditorDialogFragment :
    BaseBottomPushVMDialogFragment<DialogNickNameEditorBinding, PersonalInformationViewModel>(true) {

    companion object {
        fun show(manager: FragmentManager): NicknameEditorDialogFragment {
            val dialog = NicknameEditorDialogFragment()
            dialog.show(manager)
            return dialog
        }
    }


    private val showInput
        get() = object : Runnable {
            val edit by lazy { WeakReference(binding.etNickname) }
            override fun run() {
                edit.get()?.let {
                    KeyboardUtils.showSoftInput(it)
                    it.requestFocus()
                    it.setSelection(it.length())
                }
            }
        }

    //确定回调
    var onCommit: ((String?) -> Unit)? = null


    private var title: String? = null
    private var hint: String? = null

    fun setTitle(title: String) {
        this.title = title
    }

    fun getEditTextHint(hint: String) {
        this.hint = hint
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (title != null) {
            binding.tvTitle.text = title
        }
        if (hint != null) {
            binding.etNickname.hint = hint
        }
        //设置不可以输入特殊字符
        binding.etNickname.createInputFilter(true,10)
        binding.etNickname.post {
            binding.etNickname.setSelection(binding.etNickname.length())
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.tv_cancel -> {
                //返回
                hiddenSoftKeyboard()
                dismissAllowingStateLoss()
            }

            R.id.tv_confirm -> {
                hiddenSoftKeyboard()
                if (onCommit == null) {
                    mRealVM.commit(nickname = mRealVM.editNickname.value)
                } else {
                    onCommit?.invoke(mRealVM.editNickname.value)
                }
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ThreadUtils.runOnUiThreadDelayed(showInput, 200)
    }


    //隐藏软键盘
    private fun hiddenSoftKeyboard() {
        (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(binding.etNickname.windowToken, 0)
    }
}