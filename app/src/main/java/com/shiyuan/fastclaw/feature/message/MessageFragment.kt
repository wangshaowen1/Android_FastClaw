package com.shiyuan.fastclaw.feature.message

import androidx.recyclerview.widget.LinearLayoutManager
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.databinding.FragmentMessageBinding
import com.shiyuan.fastclaw.feature.message.chat.MessageChatActivity
import com.shiyuan.fastclaw.feature.message.data.ConversationItem
import com.shiyuan.fastclaw.feature.message.data.MessageChatParams

class MessageFragment : BaseViewModelFragment<FragmentMessageBinding, MessageViewModel>() {

    private val msgAdapter by lazy { MessageAdapter() }

    companion object {
        fun newInstance() = MessageFragment()
    }

    /**
     * 初始化消息列表 RecyclerView，并订阅 ViewModel 输出的列表数据。
     */
    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObserve()
        initData()
    }

    /**
     * 初始化消息列表与入口点击事件。
     */
    private fun initView() {
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = msgAdapter
        }
        msgAdapter.onConversationClick = {
            startChatActivity(it)
        }
        msgAdapter.onMarkedUnsuitableClick = {
            UnableMessageActivity.start(requireContext())
        }
        binding.layoutRefresh.setOnRefreshListener {
            binding.layoutRefresh.finishRefresh()
            mRealVM.getConversationList()
        }
    }

    /**
     * 监听消息列表数据变化并刷新适配器。
     */
    private fun initObserve() {
        mRealVM.conversationList.observe(viewLifecycleOwner) { items ->
            msgAdapter.submitList(items)
        }
        observeEvents {
            on<Events.UserChangedEvent> { event ->
                mRealVM.getConversationList()
            }
        }
    }

    /**
     * 标记页面进入可见状态并触发会话列表加载。
     */
    private fun initData() {
        mRealVM.getConversationList()
    }

    /**
     * 页面重新可见时刷新草稿展示状态。
     */
    override fun onResume() {
        super.onResume()
        if (!mRealVM.conversationList.value.isNullOrEmpty()) {
            mRealVM.refreshConversationList()
        }
    }

    /**
     * 启动会话聊天页面
     */
    private fun startChatActivity(item: ConversationItem) {
        MessageChatActivity.start(requireContext(), MessageChatParams(
        sessionId = item.sessionId.orEmpty(),
        contactId = item.contactId,
        name = item.targetName,
        avatar = item.targetAvatar,
        currentJobId = item.jobId,
        currentJobName = item.targetPosition,)
        )
    }
}
