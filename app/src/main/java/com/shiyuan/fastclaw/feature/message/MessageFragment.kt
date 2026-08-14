package com.shiyuan.fastclaw.feature.message

import androidx.recyclerview.widget.LinearLayoutManager
import com.nhsd.basic.BaseViewModelFragment
import com.shiyuan.fastclaw.databinding.FragmentMessageBinding

class MessageFragment : BaseViewModelFragment<FragmentMessageBinding, MessageViewModel>() {

    companion object {
        /**
         * 创建消息 Tab 页面实例，保持主 Tab Fragment 创建方式一致。
         */
        fun newInstance() = MessageFragment()
    }

    private val messageAdapter by lazy {
        MessageAdapter().apply {
            onConversationClick = { mRealVM.openConversation(it) }
            onMarkedUnsuitableClick = { mRealVM.openMarkedUnsuitable() }
        }
    }

    /**
     * 初始化消息列表 RecyclerView，并订阅 ViewModel 输出的列表数据。
     */
    override fun onPrepareView() {
        super.onPrepareView()
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
            adapter = messageAdapter
        }
        mRealVM.messageList.observe(viewLifecycleOwner) { items ->
            messageAdapter.submitList(items)
        }
    }

    /**
     * 页面重新可见时刷新会话列表。
     */
    override fun onResume() {
        super.onResume()
        mRealVM.setPageActive(true)
    }

    /**
     * 页面不可见时取消消息刷新，避免后台任务继续回写页面。
     */
    override fun onPause() {
        mRealVM.setPageActive(false)
        super.onPause()
    }
}
