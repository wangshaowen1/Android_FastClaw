package com.shiyuan.fastclaw.feature.message.chat

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.setupTranslucentStatus
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.utils.KeyboardUtils
import com.shiyuan.fastclaw.common.utils.RecyclerViewScrollHelper
import com.shiyuan.fastclaw.common.utils.KeyboardUtils.hideKeyboard
import com.shiyuan.fastclaw.common.utils.SoftKeyBoardListener
import com.shiyuan.fastclaw.databinding.ActivityMessageChatBinding
import com.shiyuan.fastclaw.feature.message.data.MessageChatParams
import com.shiyuan.fastclaw.feature.message.data.MessageDraftStore
import com.shiyuan.fastclaw.feature.message.dialog.ResumeConfirmDialog

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
    // 键盘动画运行中标记，避免普通 Insets 回调和动画回调同时修改底部间距。
    private var hasRunningImeAnimation = false
    // 当前后端本地会话业务 ID。
    private var sessionId: String = ""
    // 当前聊天对象账号。
    private var contactId: String = ""
    //HR头像
    private var targetAvatar: String? = null
    // 发简历面板是否已展开。
    private var isMorePanelVisible = false
    // 键盘收起后是否需要继续展示发简历面板。
    private var pendingShowMorePanel = false

    companion object {
        // 聊天页启动参数。
        private const val EXTRA_CHAT_PARAMS = "extra_chat_params"
        // 向上滚动接近顶部时提前加载历史消息的阈值。
        private const val PREFETCH_THRESHOLD = 5
        // 新消息平滑滚动到底部的速度，复用小虾对话的滚动手感。
        private const val MESSAGE_SCROLL_SPEED = 0.1f
        // 键盘弹起时输入区和键盘顶部的目标间距。
        private const val INPUT_KEYBOARD_GAP = 9
        // 发送按钮多行输入时距离输入框底部的间距。
        private const val SEND_BUTTON_BOTTOM_MARGIN = 6
        // 发简历面板展开和收起动画时长。
        private const val RESUME_PANEL_ANIMATION_DURATION = 100L
        // 等待键盘收起后展示发简历面板的检查间隔。
        private const val WAIT_KEYBOARD_HIDE_DELAY = 60L
        // 等待键盘收起的最大检查次数，避免部分机型不回调导致点击无响应。
        private const val WAIT_KEYBOARD_HIDE_MAX_COUNT = 6

        /**
         * 根据聊天参数打开 IM 单聊详情页。
         */
        fun start(context: Context, params: MessageChatParams) {
            val intent = Intent(context, MessageChatActivity::class.java)
                .putExtra(EXTRA_CHAT_PARAMS, params)
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
    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        binding.ivBack.setOnSingleClickListener { finish() }
        messageLayoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
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
        binding.rvMessages.setOnTouchListener { _, event ->
            if (KeyboardUtils.isSoftInputShown(window)) {
                hideKeyboard(binding.etContent)
            }
            if (isMorePanelVisible) {
                hideMorePanel()
            }
            false // 不影响 RecyclerView 正常滑动
        }
        binding.ivSend.setOnSingleClickListener {
            val hasContent = binding.etContent.text?.toString()?.isNotBlank() == true
            if (hasContent) {
                sendTextMessage()
            } else if (isMorePanelVisible) {
                hideMorePanel()
            } else {
                showMorePanel()
            }
        }
        binding.etContent.doAfterTextChanged {
            MessageDraftStore.save(sessionId, it?.toString().orEmpty())
            updateSendButton()
            updateSendPosition()
        }
        binding.layoutSendResume.setOnSingleClickListener {
            mRealVM.queryResumeForDelivery()
            hideMorePanel()
        }
        messageAdapter.onResumeRequestAcceptClick = {
            mRealVM.acceptResumeForRequest(sessionId, it)
        }
        messageAdapter.onResumeRequestRejectClick = {
            mRealVM.rejectResumeForRequest(it)
        }
        messageAdapter.onResumeFileClick = {
            //showMessage(getString(R.string.message_chat_resume_action_waiting))
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutInput) { view, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            if (!hasRunningImeAnimation) {
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = resolveBottomMargin(imeBottom)
                }
            }
            insets
        }
        ViewCompat.setWindowInsetsAnimationCallback(binding.layoutInput,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {

                //键盘动画开始时暂停普通 Insets 回调改 margin。
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        hasRunningImeAnimation = true
                    }
                }

                //键盘动画过程中逐帧更新输入区底部间距。
                override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat {
                    val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    binding.layoutInput.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = resolveBottomMargin(imeBottom)
                    }
                    return insets
                }

                //键盘动画结束后恢复普通 Insets 回调。
                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        hasRunningImeAnimation = false
                        ViewCompat.requestApplyInsets(binding.layoutInput)
                        showMorePanelAfterHidden()
                    }
                }
            }
        )
        SoftKeyBoardListener(this).setOnSoftKeyBoardChangeListener(object : SoftKeyBoardListener.OnSoftKeyBoardChangeListener {

            //键盘真正展开后隐藏发简历面板，避免和输入法同时占位。
            override fun keyBoardShow(height: Int) {
                hideMorePanel(false)
            }

            //键盘收起时不再延迟修正 Insets，避免动画结束后再次跳动。
            override fun keyBoardHide(height: Int) {
                hasRunningImeAnimation = false
                showMorePanelAfterHidden()
            }
        })
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
            val scrollToLatest = forceScrollToBottom || RecyclerViewScrollHelper.isScrollBottom(
                binding.rvMessages,
                binding.rvMessages.paddingBottom
            )
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
        mRealVM.resumeForDelivery.observe(this) { resume ->
            if (resume == null || !resume.hasLatestResume) {
                showToast(R.string.message_chat_resume_empty_tip)
                return@observe
            }
            ResumeConfirmDialog.show(supportFragmentManager, resume) {
                forceScrollToBottom = true
                mRealVM.sendResumeDeliveryMessage(it)
            }
        }
    }

    /**
     * 读取会话参数并初始化 IM 单聊。
     */
    @Suppress("DEPRECATION")
    private fun initData() {
        val params = intent.getParcelableExtra<MessageChatParams>(EXTRA_CHAT_PARAMS)
        binding.tvTitle.text = params?.name.orEmpty()
        sessionId = params?.sessionId.orEmpty()
        contactId = params?.contactId.orEmpty()
        targetAvatar = params?.avatar
        mRealVM.initChat(contactId, targetAvatar, params?.isFirstCreate)
        restoreMessageDraft()
    }


    /**
     * 计算输入区相对父容器底部应有的间距，让输入框和键盘顶部保持固定间距。
     */
    private fun resolveBottomMargin(imeBottom: Int): Int {
        return (imeBottom - binding.layoutContainer.paddingBottom + INPUT_KEYBOARD_GAP.dp).coerceAtLeast(0)
    }

    /**
     * 展开发简历面板并把右侧按钮切成关闭态。
     */
    private fun showMorePanel() {
        if (isMorePanelVisible) return
        val keyboardShown = KeyboardUtils.isImeVisible(binding.root)
        hideKeyboard(binding.etContent)
        binding.etContent.clearFocus()
        if (keyboardShown) {
            pendingShowMorePanel = true
            updateSendButton()
            showMorePanelAfterHidden(0)
            return
        }
        showMorePanelDirectly()
    }

    /**
     * 隐藏发简历面板并恢复默认输入框高度。
     */
    private fun hideMorePanel(clearPending: Boolean = true) {
        if (clearPending) {
            pendingShowMorePanel = false
        }
        if (!isMorePanelVisible) return
        isMorePanelVisible = false
        animateResumePanelChange()
        binding.layoutMore.visibility = View.GONE
        updateSendButton()
    }

    /**
     * 立即展开发简历面板。
     */
    private fun showMorePanelDirectly() {
        isMorePanelVisible = true
        animateResumePanelChange()
        binding.layoutMore.visibility = View.VISIBLE
        updateSendButton()
    }

    /**
     * 键盘完全收起后再展开发简历面板，避免键盘动画和面板动画互相抢布局。
     */
    private fun showMorePanelAfterHidden(waitCount: Int = 0) {
        if (!pendingShowMorePanel) return
        if (KeyboardUtils.isImeVisible(binding.root) && waitCount < WAIT_KEYBOARD_HIDE_MAX_COUNT) {
            binding.layoutInput.postDelayed({
                showMorePanelAfterHidden(waitCount + 1)
            }, WAIT_KEYBOARD_HIDE_DELAY)
            return
        }
        pendingShowMorePanel = false
        if (!isMorePanelVisible) {
            showMorePanelDirectly()
        }
    }

    /**
     * 使用布局过渡动画处理发简历面板展开和收起。
     */
    private fun animateResumePanelChange() {
        TransitionManager.beginDelayedTransition(binding.layoutContainer, ChangeBounds().apply {
                duration = RESUME_PANEL_ANIMATION_DURATION
            }
        )
    }

    /**
     * 根据输入框是否有内容切换发送按钮样式。
     */
    private fun updateSendButton() {
        val hasContent = binding.etContent.text?.toString()?.isNotBlank() == true
        val iconRes = when {
            hasContent -> R.drawable.ic_message_send_active
            isMorePanelVisible || pendingShowMorePanel -> R.drawable.ic_message_send_inactive
            else -> R.drawable.ic_message_add_image
        }
        binding.ivSend.setImageResource(iconRes)
    }

    /**
     * 根据输入框行数调整发送按钮位置，一行居中，多行贴近输入框底部。
     */
    private fun updateSendPosition() {
        binding.etContent.post {
            val isMultiLine = binding.etContent.lineCount > 1
            binding.ivSend.updateLayoutParams<ConstraintLayout.LayoutParams> {
                verticalBias = if (isMultiLine) 1f else 0.5f
                bottomMargin = if (isMultiLine) SEND_BUTTON_BOTTOM_MARGIN.dp else 0
            }
        }
    }

    /**
     * 发送输入框中的文本消息。
     */
    private fun sendTextMessage() {
        val content = binding.etContent.text?.toString().orEmpty()
        if (content.isBlank()) return
        forceScrollToBottom = true
        mRealVM.sendTextMessage(content)
        MessageDraftStore.clear(sessionId)
        binding.etContent.text?.clear()
    }

    /**
     * 恢复当前会话输入框草稿。
     */
    private fun restoreMessageDraft() {
        MessageDraftStore.get(sessionId)?.let { draft ->
            binding.etContent.setText(draft)
            binding.etContent.setSelection(binding.etContent.text?.length ?: 0)
        }
    }

    /**
     * 系统返回键优先收起发简历面板，面板未展示时再关闭页面。
     */
    override fun onBackPressed() {
        if (isMorePanelVisible) {
            hideMorePanel()
            return
        }
        super.onBackPressed()
    }

    /**
     * 设置透明状态栏并保持深色状态栏字体。
     */
    override fun setTranslucentStatus() {
        setupTranslucentStatus(true, ContextCompat.getColor(this, R.color.color_f6f9ff))
    }
}
