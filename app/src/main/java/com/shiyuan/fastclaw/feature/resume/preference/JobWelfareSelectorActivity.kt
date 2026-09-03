package com.shiyuan.fastclaw.feature.resume.preference

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
import com.shiyuan.fastclaw.common.utils.JsonUtil
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumTreeUtil
import com.shiyuan.fastclaw.databinding.ActivityJobWelfareSelectorBinding
import kotlinx.serialization.json.Json

/**
 * @desc：福利偏好选择
 * @author：CaoWei
 * @date：2026/6/12 16:25
 */
class JobWelfareSelectorActivity : BaseViewModelActivity<ActivityJobWelfareSelectorBinding, JobWelfareSelectorModel>() {

    // 候选福利网格适配器，传入选中判断和切换回调
    private val welfareAdapter by lazy {
        JobWelfareAdapter(
            isSelected = { item -> item.code?.let(selectedWelfare::containsKey) == true },
            onSelected = { item -> toggleWelfare(item) },
        )
    }
    // 底部已选福利横滑标签适配器
    private val selectedAdapter by lazy {
        SelectedWelfareAdapter { item -> removeSelectedWelfare(item) }
    }
    // 已选福利集合，LinkedHashMap 保证按选中顺序展示且方便按键去重
    private val selectedWelfare = linkedMapOf<String, CommonEnumItem>()
    // 候选福利原始数据
    private var welfareItems = emptyList<CommonEnumItem>()
    // 福利偏好最大可选数量
    private val maxSelectCount = 10
    // 会话 ID / 对话 ID
    private val sessionId by lazy { intent.getStringExtra(EXTRA_SESSION_ID).orEmpty() }
    private val conversationId by lazy { intent.getStringExtra(EXTRA_CONVERSATION_ID).orEmpty() }
    private val resultEventType by lazy { intent.getStringExtra(EXTRA_RESULT_EVENT_TYPE).orEmpty() }
    private val initialSelectedWelfare by lazy {
        Json.decodeFromString<List<CommonEnumItem>>(intent.getStringExtra(EXTRA_SELECTED_WELFARE) ?: "[]")
    }

    companion object {
        // Intent 参数 key
        private const val EXTRA_SESSION_ID = "extra_session_id"
        private const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        private const val EXTRA_RESULT_EVENT_TYPE = "extra_result_event_type"
        private const val EXTRA_SELECTED_WELFARE = "extra_selected_welfare"

        /**
         * 打开福利偏好选择器页面。
         *
         * @param activity 当前页面 Activity。
         * @param sessionId 求职 Agent 会话 ID，用于接口反显当前会话已选福利。
         * @param conversationId 对话 ID，用于校验当前会话归属。
         * @param resultEventType 调用方指定的回传事件类型，为空时保持原有无类型事件。
         * @param selectedValueJson 调用方传入的已选福利 JSON，非空时优先用于回显。
         */
        fun start(
            activity: Activity?,
            sessionId: String? = null,
            conversationId: String? = null,
            resultEventType: String? = null,
            selectedValueJson: String? = null,
        ) {
            activity ?: return
            val intent = Intent(activity, JobWelfareSelectorActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_RESULT_EVENT_TYPE, resultEventType)
                putExtra(EXTRA_SELECTED_WELFARE, selectedValueJson ?: "[]")
            }
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 监听数据变化
        initObserve()
        // 初始化视图与点击事件
        initView()
    }

    override fun setTranslucentStatus() {
        // 设置沉浸式状态栏
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
        initialSelectedWelfare.forEach { item ->
            item.code?.takeIf { it.isNotBlank() }?.let { selectedWelfare[it] = item }
        }
        // 候选福利网格（三列）
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
        // 底部已选福利横滑列表
        binding.rvSelectedWelfare.apply {
            layoutManager = LinearLayoutManager(
                this@JobWelfareSelectorActivity,
                RecyclerView.HORIZONTAL,
                false,
            )
            itemAnimator = null
            adapter = selectedAdapter
        }
        // 关闭按钮
        binding.ivClose.setOnSingleClickListener { finish() }
        // 完成按钮：未选时提示，否则把已选福利回传给上游
        binding.btnComplete.setOnSingleClickListener {
            if (selectedWelfare.isEmpty()) {
                getString(R.string.welfare_selector_at_least).showToast()
                return@setOnSingleClickListener
            }
            val msgGson = CommonEnumTreeUtil.toJson(selectedWelfare)
            val event = if (resultEventType.isBlank()) {
                Events.ClawAgentEvent(msgGson)
            } else {
                Events.ClawAgentEvent(msgGson, resultEventType)
            }
            FlowEventBus.postSticky(event)
            finish()
        }
        // 初始化已选区域
        refreshSelectedState()
        // 拉取福利字典（含已选反显）
        mRealVM.queryWelfareDict(sessionId, conversationId)
    }

    /**
     * 监听福利偏好字典接口数据，刷新候选项和服务端反显的已选项。
     */
    private fun initObserve() {
        mRealVM.welfareDict.observe(this) { data ->
            // 每次刷新都重置已选集合，优先按调用方传入数据回填，否则使用接口数据
            selectedWelfare.clear()
            initialSelectedWelfare.ifEmpty { data.selectedItems }.forEach { item ->
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
            // 已选 → 取消
            selectedWelfare.remove(code)
        } else {
            // 未选 → 校验上限
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
        // 已选数量 / 上限
        binding.tvCount.text = selectedWelfare.size.toString()
        binding.tvMax.text = "/$maxSelectCount"
        // 未选时禁用完成按钮并半透明灰化
        binding.btnComplete.isEnabled = selectedWelfare.isNotEmpty()
        binding.btnComplete.alpha = if (selectedWelfare.isNotEmpty()) 1f else 0.3f
        selectedAdapter.submitList(selectedWelfare.values.toList())
    }
}
