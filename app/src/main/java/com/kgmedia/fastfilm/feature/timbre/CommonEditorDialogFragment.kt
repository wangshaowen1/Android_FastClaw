package com.kgmedia.fastfilm.feature.timbre

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ThreadUtils
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.databinding.DialogCommonEditorBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import java.lang.ref.WeakReference

/**
 * 通用修改弹窗
 */
class CommonEditorDialogFragment :
    BaseBottomPushVMDialogFragment<DialogCommonEditorBinding, CommonEditorDialogViewModel>() {

    companion object {
        const val TITLE = "title"
        const val HINT = "hint"
        const val LIMIT_THE_WORD_COUNT = "limitTheWordCount"

        const val TYPE_TIMBRE_NAME = 1

        /**
         * 修改文字属性
         * @param manager FragmentManager
         * @param title String? 标题
         * @param hint String? 提示语
         * @param limitTheWordCount Int 限制最大字数
         * @param id 业务相关主键id
         * @param type 业务类型（有值的情况更新接口可以在这里执行，不需要的话可以不用传），1 修改音色名称
         * @return CommonEditorDialogFragment
         */
        fun show(
            manager: FragmentManager,
            title: String? = null,
            hint: String? = null,
            limitTheWordCount: Int = 10,
            id: String? = null,
            type: Int? = null,
        ): CommonEditorDialogFragment {
            val dialog = CommonEditorDialogFragment()
            dialog.arguments = Bundle().apply {
                putString(TITLE, title)
                putString(HINT, hint)
                putInt(LIMIT_THE_WORD_COUNT, limitTheWordCount)
                id?.let { putString(Constants.Extra.ID, id) }
                type?.let { putInt(Constants.Extra.TYPE, it) }
            }
            dialog.show(manager)
            return dialog
        }
    }

    private var title: String? = null
    private var hint: String? = null
    private var limitTheWordCount: Int = 10
    private var type: Int? = null

    private val showInput
        get() = object : Runnable {
            val edit by lazy { WeakReference(binding.etContent) }
            override fun run() {
                edit.get()?.let {
                    KeyboardUtils.showSoftInput(it)
                    it.requestFocus()
                    it.setSelection(it.length())
                }
            }
        }

//    public override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val dialog = Dialog(requireActivity(), R.style.Dialog_Right_BottomPush)
//        return dialog
//    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window
        val params = window?.attributes
        params?.gravity = Gravity.BOTTOM // 显示在底部
        params?.width = WindowManager.LayoutParams.MATCH_PARENT // 宽度填充满屏
        window?.attributes = params

        window?.let {
            // 3) 允许我们控制系统栏（状态栏/导航栏）显示与隐藏（edge-to-edge）
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // 4) 隐藏系统栏（使用 AndroidX 辅助类，兼容更好）
            val decorView = window.decorView
            val controller = WindowInsetsControllerCompat(window, decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // 可设置沉浸式交互（用户手势后可短暂显示）
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    var onModifySuccess: ((Pair<String?, String?>) -> Unit)? = null

    override fun onPrepareView() {
        super.onPrepareView()
        arguments?.apply {
            title = getString(TITLE)
            hint = getString(HINT)
            limitTheWordCount = getInt(LIMIT_THE_WORD_COUNT)
            type = getInt(Constants.Extra.TYPE)
        }
        if (title != null) {
            binding.tvTitle.text = title
        }
        if (hint != null) {
            binding.etContent.hint = hint
        }
        //设置不可以输入特殊字符
        binding.etContent.createInputFilter(true, limitTheWordCount)
        binding.tvNo.setOnSingleClickListener {
            //返回
            hiddenSoftKeyboard()
            dismissAllowingStateLoss()
        }

        binding.tvYes.setOnSingleClickListener {
            //保存
            hiddenSoftKeyboard()
            if (type == TYPE_TIMBRE_NAME) {
                mRealVM.modifyTimbreName {
                    onModifySuccess?.invoke(mRealVM.id to mRealVM.content.value)
                    dismissAllowingStateLoss()
                }
            } else {
                onModifySuccess?.invoke(null to mRealVM.content.value)
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
            ?.hideSoftInputFromWindow(binding.etContent.windowToken, 0)
    }
}