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
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ScreenUtils
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.databinding.FragmentClawAgentMainBinding
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
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

    private val msgAdapter by lazy { ClawAgentMsgAdapter() }

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
                mRealVM.sendMessage(content.toString().trim())
                binding.etSend.text = null
            }
        }
    }

    private fun initView() {
        // 消息列表：聊天场景内容自底部对齐
        binding.rvMsg.layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.rvMsg.addItemDecoration(SpaceDecoration(12.dp, SpaceDecoration.Direction.TOP_WITH_BOTTOM))
        binding.rvMsg.adapter = msgAdapter

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
        mRealVM.enterSession()

        mRealVM.messageList.observe(viewLifecycleOwner) { list ->
            // 提交前判断是否已在底部：是则随新消息/流式增量自动滚到最新，否则保留浏览历史的位置
            val atBottom = !binding.rvMsg.canScrollVertically(1)
            msgAdapter.submitList(list)
            if (atBottom && list.isNotEmpty()) {
                binding.rvMsg.scrollToPosition(list.size - 1)
            }
        }
        mRealVM.streamError.observe(viewLifecycleOwner) { e ->
            e ?: return@observe
            showMessage(e.message ?: "对话出错")
        }
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