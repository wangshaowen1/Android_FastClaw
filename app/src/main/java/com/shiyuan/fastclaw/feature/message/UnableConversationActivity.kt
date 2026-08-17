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

/**
 * 已标记不合适会话列表页，承接消息 Tab 中“不合适会话”入口。
 */
class UnableConversationActivity : BaseViewModelActivity<ActivityUnableConversationBinding, MessageViewModel>() {

    private val conversationAdapter by lazy { MessageAdapter() }

    companion object {
        /**
         * 打开已标记不合适会话列表页。
         */
        fun start(context: Context) {
            context.startActivity(Intent(context, UnableConversationActivity::class.java))
        }
    }

    /**
     * 页面创建后初始化列表与数据监听。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObserve()
    }

    /**
     * 初始化返回按钮、会话列表与列表点击事件。
     */
    private fun initView() {
        binding.ivBack.setOnSingleClickListener { finish() }
        binding.rvConversations.apply {
            layoutManager = LinearLayoutManager(this@UnableConversationActivity)
            adapter = conversationAdapter
        }
        conversationAdapter.onConversationClick = {
            mRealVM.openConversation(it)
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
     * 页面可见时刷新已标记不合适的会话。
     */
    override fun onResume() {
        super.onResume()
        mRealVM.setPageState(true)
    }

    /**
     * 页面不可见时暂停会话刷新。
     */
    override fun onPause() {
        mRealVM.setPageState(false)
        super.onPause()
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
