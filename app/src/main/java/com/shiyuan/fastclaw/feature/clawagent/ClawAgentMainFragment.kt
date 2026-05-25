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
import androidx.recyclerview.widget.RecyclerView
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

        /** 距顶部多少条时开始预取更早历史 */
        private const val PREFETCH_THRESHOLD = 5

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
                if (mRealVM.streaming.value == true) {
                    mRealVM.cancelStream(flag = false)
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

        // 无感知加载：向上滚动到接近顶部时提前预取更早历史（DiffUtil 顶部静默插入，不出现下拉转圈）
        binding.rvMsg.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy >= 0) return // 仅在向上滚动时预取
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                if (lm.findFirstVisibleItemPosition() <= PREFETCH_THRESHOLD) {
                    mRealVM.loadOlderHistory()
                }
            }
        })

        // 下拉加载更早历史消息（顶部下拉）
        binding.srlMsg.setOnRefreshListener {
            // 进入失败，下拉调用进入会话
            if (mRealVM.sessionData.value == null) {
                mRealVM.enterSession()
                return@setOnRefreshListener
            }
//            mRealVM.loadOlderHistory()
        }

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
        mRealVM.historyLoadFinished.observe(viewLifecycleOwner) { hasMore ->
            // 结束下拉刷新；没有更早消息则关闭下拉手势
            binding.srlMsg.finishRefresh()
            binding.srlMsg.setEnableRefresh(hasMore)
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