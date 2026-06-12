package com.shiyuan.fastclaw.feature.information

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumTreeUtil
import com.shiyuan.fastclaw.databinding.ActivityJobWelfareSelectorBinding
import com.shiyuan.fastclaw.feature.information.adapter.JobWelfareAdapter
import com.shiyuan.fastclaw.feature.information.adapter.SelectedWelfareAdapter
import com.shiyuan.fastclaw.feature.information.viewModel.JobWelfareSelectorModel

/**
 * @desc：福利偏好选择
 * @author：CaoWei
 * @date：2026/6/12 16:25
 */
class JobWelfareSelectorActivity : BaseViewModelActivity<ActivityJobWelfareSelectorBinding, JobWelfareSelectorModel>() {

    private val selectedWelfare = linkedMapOf<String, CommonEnumItem>()
    private var welfareItems = emptyList<CommonEnumItem>()
    private val maxSelectCount = 10
    private val sessionId by lazy { intent.getStringExtra(EXTRA_SESSION_ID).orEmpty() }
    private val conversationId by lazy { intent.getStringExtra(EXTRA_CONVERSATION_ID).orEmpty() }
    private val welfareAdapter by lazy {
        JobWelfareAdapter(
            isSelected = { item -> item.code?.let(selectedWelfare::containsKey) == true },
            onSelected = { item -> toggleWelfare(item) },
        )
    }
    private val selectedAdapter by lazy {
        SelectedWelfareAdapter { item -> removeSelectedWelfare(item) }
    }

    companion object {
        private const val EXTRA_SESSION_ID = "extra_session_id"
        private const val EXTRA_CONVERSATION_ID = "extra_conversation_id"

        /**
         * 打开福利偏好选择器页面。
         *
         * @param activity 当前页面 Activity。
         * @param sessionId 求职 Agent 会话 ID，用于接口反显当前会话已选福利。
         * @param conversationId 对话 ID，用于校验当前会话归属。
         */
        fun start(activity: Activity?, sessionId: String? = null, conversationId: String? = null) {
            activity ?: return
            val intent = Intent(activity, JobWelfareSelectorActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
            }
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserve()
        initView()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

    /**
     * 初始化页面列表、点击事件、完成按钮状态，并请求福利偏好字典数据。
     */
    private fun initView() {
        binding.rvWelfare.apply {
            layoutManager = GridLayoutManager(
                this@JobWelfareSelectorActivity,
                3,
                RecyclerView.VERTICAL,
                false,
            )
            itemAnimator = null
            adapter = welfareAdapter
        }
        binding.rvSelectedWelfare.apply {
            layoutManager = LinearLayoutManager(
                this@JobWelfareSelectorActivity,
                RecyclerView.HORIZONTAL,
                false,
            )
            itemAnimator = null
            adapter = selectedAdapter
        }
        binding.ivClose.setOnSingleClickListener { finish() }
        binding.btnComplete.setOnSingleClickListener {
            if (selectedWelfare.isEmpty()) {
                "至少选择一个福利偏好".showToast()
                return@setOnSingleClickListener
            }
            val msgGson = CommonEnumTreeUtil.toJson(selectedWelfare)
            FlowEventBus.postSticky(Events.ClawAgentEvent(msgGson))
            finish()
        }
        refreshSelectedState()
        mRealVM.queryWelfareDict(sessionId, conversationId)
    }

    /**
     * 监听福利偏好字典接口数据，刷新候选项和服务端反显的已选项。
     */
    private fun initObserve() {
        mRealVM.welfareDict.observe(this) { data ->
            selectedWelfare.clear()
            data.selectedItems.forEach { item ->
                item.code?.takeIf { it.isNotBlank() }?.let { selectedWelfare[it] = item }
            }
            welfareItems = data.items
            renderWelfareGrid()
            refreshSelectedState()
        }
    }

    /**
     * 根据当前福利候选列表渲染中间的三列标签网格。
     */
    private fun renderWelfareGrid() {
        welfareAdapter.submitList(welfareItems.map { it })
    }

    /**
     * 切换福利偏好的选中状态，并处理最大可选数量限制。
     */
    private fun toggleWelfare(item: CommonEnumItem) {
        val code = item.code
        if (code.isNullOrBlank()) return
        if (selectedWelfare.containsKey(code)) {
            selectedWelfare.remove(code)
        } else {
            if (selectedWelfare.size >= maxSelectCount) {
                "最多选择${maxSelectCount}个".showToast()
                return
            }
            selectedWelfare[code] = item
        }
        welfareAdapter.notifyDataSetChanged()
        refreshSelectedState()
    }

    /**
     * 从底部已选区域移除指定福利，并同步候选网格选中态。
     */
    private fun removeSelectedWelfare(item: CommonEnumItem) {
        val code = item.code
        if (code.isNullOrBlank()) return
        selectedWelfare.remove(code)
        welfareAdapter.notifyDataSetChanged()
        refreshSelectedState()
    }

    /**
     * 刷新已选数量、完成按钮可用状态，以及底部已选福利标签列表。
     */
    private fun refreshSelectedState() {
        binding.tvCount.text = selectedWelfare.size.toString()
        binding.tvMax.text = "/$maxSelectCount"
        binding.btnComplete.isEnabled = selectedWelfare.isNotEmpty()
        binding.btnComplete.alpha = if (selectedWelfare.isNotEmpty()) 1f else 0.3f
        selectedAdapter.submitList(selectedWelfare.values.toList())
    }
}
