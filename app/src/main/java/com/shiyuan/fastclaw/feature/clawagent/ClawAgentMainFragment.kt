package com.shiyuan.fastclaw.feature.clawagent

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.blankj.utilcode.util.ScreenUtils
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.databinding.FragmentClawAgentMainBinding
import org.w3c.dom.Text

/** 
 * @createtime 2026年05月20日 10:25
 * @author TooWhiteT
 */

class ClawAgentMainFragment : BaseViewModelFragment<FragmentClawAgentMainBinding, ClawAgentViewModel>() {

    companion object {
        const val TAG = "ClawAgentMainFragment"

        fun getInstance(): ClawAgentMainFragment {
            val fragment = ClawAgentMainFragment()
//            fragment.arguments = Bundle().also {
//
//            }
            return fragment
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        observeData()
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.iv_send -> {
                val content = binding.etSend.text
                if (content.isBlank()) {
                    showMessage("发送内容不能为空")
                    return
                }
            }
        }
    }

    private fun initView() {
        binding.ivAgentAvatar.setOnClickListener {

        }

        binding.etSend.doAfterTextChanged { editable ->
            editable?.let {
                if (it.isNotEmpty()) {
                    binding.ivSend.visible()
                } else {
                    binding.ivSend.gone()
                }
            } ?: run {
                binding.ivSend.gone()
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.etSend) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (ime.bottom <= 0) {
                    bottomMargin = 12.dp
                } else {
                    bottomMargin = ime.bottom - 90.dp
                }
            }
            insets
        }
    }

    private fun observeData() {

    }

    // 记忆卡片
    private var popup: PopupWindow? = null

    private fun showMemoryCardPopup() {
        val contentView = LayoutInflater.from(context).inflate(R.layout.popup_memory_card_select, null, false)
        val confirmBtn = contentView.findViewById<TextView>(R.id.tv_confirm)
        val negationBtn = contentView.findViewById<TextView>(R.id.tv_negation)
        confirmBtn.setOnClickListener {
            dismissMemoryCard()
        }
        negationBtn.setOnClickListener {
            dismissMemoryCard()
        }
        contentView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        popup = PopupWindow(context)
        popup?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popup?.contentView = contentView
        popup?.width = ScreenUtils.getScreenWidth()
        popup?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popup?.isFocusable = false
        popup?.isOutsideTouchable = false
        popup?.showAtLocation(binding.root, Gravity.TOP, 0,0)
    }

    private fun dismissMemoryCard() {
        popup?.dismiss()
        popup = null
    }
}