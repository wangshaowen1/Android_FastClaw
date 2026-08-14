package com.shiyuan.fastclaw.feature.message

import androidx.recyclerview.widget.LinearLayoutManager
import com.nhsd.basic.BaseViewModelFragment
import com.shiyuan.fastclaw.databinding.FragmentMessageBinding
import com.shiyuan.fastclaw.feature.clawagent.ClawAgentMsgAdapter

class MessageFragment : BaseViewModelFragment<FragmentMessageBinding, MessageViewModel>() {

    private val msgAdapter by lazy { MessageAdapter() }

    companion object {
        /**
         * 创建消息 Tab 页面实例，保持主 Tab Fragment 创建方式一致。
         */
        fun newInstance() = MessageFragment()
    }

    /**
     * 初始化消息列表 RecyclerView，并订阅 ViewModel 输出的列表数据。
     */
    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObserve()
    }

    private fun initView() {
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = msgAdapter
        }
        msgAdapter.onConversationClick = {
            mRealVM.openConversation(it)
        }
        msgAdapter.onMarkedUnsuitableClick = {
            mRealVM.openMarkedUnsuitable()
        }
    }

    private fun initObserve() {
        mRealVM.messageList.observe(viewLifecycleOwner) { items ->
            msgAdapter.submitList(items)
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
