package com.shiyuan.fastclaw.feature.message

import androidx.recyclerview.widget.LinearLayoutManager
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.FragmentMessageBinding
import com.shiyuan.fastclaw.feature.message.chat.MessageChatActivity
import com.tulele.im.Chat
import com.tulele.im.ImSession
import com.tulele.im.entity.Contact
import com.tulele.im.entity.MessageWrapper

class MessageFragment : BaseViewModelFragment<FragmentMessageBinding, MessageViewModel>() {

    private val msgAdapter by lazy { MessageAdapter() }

    companion object {
        // 测试消息接收账号。
        private const val TEST_IM_ACCOUNT = "123456"

        // 测试消息内容。
        private const val TEST_IM_CONTENT = "消息消息消息消息消息消息消息消息消息消息消息消息消息消息消息消息消息消息消息消息消息消息消息消息"

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
            MessageChatActivity.start(requireContext(), it.contactId, it.targetName, it.targetAvatar)
        }
        msgAdapter.onMarkedUnsuitableClick = {
            UnableMessageActivity.start(requireContext())
        }
        binding.sbTest.setOnSingleClickListener {
            sendTestImMessage()
        }
    }

    /**
     * 监听消息列表数据变化并刷新适配器。
     */
    private fun initObserve() {
        mRealVM.conversationList.observe(viewLifecycleOwner) { items ->
            msgAdapter.submitList(items)
        }
    }

    /**
     * 标记页面进入可见状态并触发会话列表加载。
     */
    private fun initData() {
        mRealVM.getConversationList()
    }

    /**
     * 发送 IM 测试文本消息，用于验证云信 V2 单聊发送链路。
     */
    private fun sendTestImMessage() {
        var chat: Chat? = null
        try {
            chat = ImSession.get().openChat(Contact(TEST_IM_ACCOUNT))
            chat.onMessageSendResultListener = object : Chat.OnMessageSendResultListener {
                override fun onSendSuccess(message: MessageWrapper, antispamResult: String?) {
                    showMessage("IM 测试消息发送成功")
                    //mRealVM.getConversationList()
                    chat?.close()
                }

                override fun onSendFailed(message: MessageWrapper, errorCode: Int, throwable: Throwable?) {
                    showMessage("IM 测试消息发送失败：$errorCode")
                    chat?.close()
                }
            }
            chat.sendTextMessage(TEST_IM_CONTENT)
        } catch (exception: Exception) {
            chat?.close()
            showMessage("IM 测试消息发送异常：${exception.message.orEmpty()}")
        }
    }
}
