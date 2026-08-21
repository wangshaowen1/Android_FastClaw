package com.shiyuan.fastclaw.feature.message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.databinding.ActivityUnableConversationBinding
import com.shiyuan.fastclaw.feature.message.chat.MessageChatActivity

/**
 * 已标记不合适会话列表页，承接消息 Tab 中“不合适会话”入口。
 */
class UnableMessageActivity : BaseViewModelActivity<ActivityUnableConversationBinding, UnableMessageViewModel>() {

    private val conversationAdapter by lazy { MessageAdapter() }

    companion object {
        /**
         * 打开已标记不合适会话列表页。
         */
        fun start(context: Context) {
            context.startActivity(Intent(context, UnableMessageActivity::class.java))
        }
    }

    /**
     * 页面创建后初始化列表与数据监听。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObserve()
        initData()
    }

    /**
     * 初始化返回按钮、会话列表与列表点击事件。
     */
    private fun initView() {
        binding.ivBack.setOnSingleClickListener { finish() }
        binding.rvConversations.apply {
            layoutManager = LinearLayoutManager(this@UnableMessageActivity)
            adapter = conversationAdapter
        }
        conversationAdapter.onConversationClick = {
            MessageChatActivity.start(this, it.contactId, it.targetName, it.targetAvatar)
        }
    }

    /**
     * 监听 ViewModel 列表数据并刷新适配器。
     */
    private fun initObserve() {
        mRealVM.conversationList.observe(this) { items ->
            conversationAdapter.submitList(items)
        }
    }

    /**
     * 标记页面进入可见状态并触发会话列表加载。
     */
    private fun initData() {
        mRealVM.getConversationList()
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
