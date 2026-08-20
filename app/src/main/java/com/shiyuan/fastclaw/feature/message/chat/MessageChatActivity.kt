package com.shiyuan.fastclaw.feature.message.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.utils.RecyclerViewScrollHelper
import com.shiyuan.fastclaw.databinding.ActivityMessageChatBinding
import com.shiyuan.fastclaw.feature.message.data.ConversationItem

/**
 * IM 单聊详情页，承接消息会话列表进入后的聊天展示和文本发送。
 */
class MessageChatActivity : BaseViewModelActivity<ActivityMessageChatBinding, MessageChatViewModel>() {

    // 单聊消息列表适配器。
    private val messageAdapter by lazy { MessageChatAdapter() }
    // 单聊消息列表布局管理器。
    private lateinit var messageLayoutManager: LinearLayoutManager
    // 下一次列表刷新后是否滚动到底部。
    private var forceScrollToBottom = false

    private var contactId: String = ""

    companion object {
        // 接收方账号参数。
        private const val EXTRA_CONTACT_ID = "extra_contact_id"
        // 接收方昵称参数。
        private const val EXTRA_TARGET_NAME = "extra_target_name"
        // 向上滚动接近顶部时提前加载历史消息的阈值。
        private const val PREFETCH_THRESHOLD = 5
        // 新消息平滑滚动到底部的速度，复用小虾对话的滚动手感。
        private const val MESSAGE_SCROLL_SPEED = 0.1f

        /**
         * 从会话列表打开 IM 单聊详情页。
         */
        fun start(context: Context, item: ConversationItem) {
            val intent = Intent(context, MessageChatActivity::class.java)
                .putExtra(EXTRA_CONTACT_ID, item.contactId)
                .putExtra(EXTRA_TARGET_NAME, item.targetName)
            context.startActivity(intent)
        }
    }

    /**
     * 页面创建后初始化聊天 UI、消息监听和 IM 会话。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObserve()
        initData()
    }

    /**
     * 初始化返回按钮、消息列表、输入框和卡片预留点击事件。
     */
    private fun initView() {
        binding.ivBack.setOnSingleClickListener { finish() }
        messageLayoutManager = LinearLayoutManager(this)
        binding.rvMessages.apply {
            layoutManager = messageLayoutManager
            adapter = messageAdapter
            itemAnimator = null
        }
        // 无感知加载：向上滚动到接近顶部时提前预取更早历史（DiffUtil 顶部静默插入，不出现下拉转圈）
        binding.rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy >= 0) return // 仅在向上滚动时预取
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                if (lm.findFirstVisibleItemPosition() <= PREFETCH_THRESHOLD) {
                    mRealVM.loadMoreList()
                }
            }
        })
        binding.ivSend.setOnSingleClickListener {
            sendTextMessage()
        }
        messageAdapter.onResumeRequestAcceptClick = {
            showMessage(getString(R.string.message_chat_resume_action_waiting))
        }
        messageAdapter.onResumeRequestRejectClick = {
            showMessage(getString(R.string.message_chat_resume_action_waiting))
        }
        messageAdapter.onResumeFileClick = {
            showMessage(getString(R.string.message_chat_resume_action_waiting))
        }
    }

    /**
     * 监听 ViewModel 消息列表变化并刷新适配器。
     */
    private fun initObserve() {
        mRealVM.messageList.observe(this) { items ->
            // 是否滚到最新：用户刚发送(forceScrollToBottom)强制滚动；否则仅在原本已在底部时
            // 跟随新消息，避免打断向上浏览历史。判定须在 submitList 之前取(反映旧位置)。
            // 如果用户在底部，就跟随新消息滚到底；如果用户往上翻历史，就不打断。
            val hadMessages = messageAdapter.items.isNotEmpty()
            val hasNewTailMessage = items.size > messageAdapter.items.size
            val scrollToLatest = forceScrollToBottom || !binding.rvMessages.canScrollVertically(1)
            forceScrollToBottom = false
            messageAdapter.submitList(items)
            if (items.isNotEmpty() && scrollToLatest) {
                // 首次加载和普通状态刷新直接贴底；新增尾部消息复用小虾对话的平滑滚动动画。
                if (hadMessages && hasNewTailMessage) {
                    RecyclerViewScrollHelper.smoothScrollToBottom(binding.rvMessages, 100L, MESSAGE_SCROLL_SPEED)
                } else {
                    RecyclerViewScrollHelper.scrollToBottom(binding.rvMessages, 100L)
                }
            }
        }
    }

    /**
     * 读取会话参数并初始化 IM 单聊。
     */
    private fun initData() {
        contactId = intent.getStringExtra(EXTRA_CONTACT_ID) ?: ""
        binding.tvTitle.text = intent.getStringExtra(EXTRA_TARGET_NAME).orEmpty()
        mRealVM.initChat(contactId)
    }

    /**
     * 发送输入框中的文本消息。
     */
    private fun sendTextMessage() {
        val content = binding.etContent.text?.toString().orEmpty()
        if (content.isBlank()) return
        forceScrollToBottom = true
        mRealVM.sendTextMessage(content)
        binding.etContent.text?.clear()
    }

    /**
     * 设置透明状态栏并保持深色状态栏字体。
     */
    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_f6f9ff)
            .transparentStatusBar()
            .init()
    }
}
