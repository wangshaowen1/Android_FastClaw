package com.shiyuan.fastclaw.feature.information

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumTreeUtil
import com.shiyuan.fastclaw.core.network.apis.response.JobCityData
import com.shiyuan.fastclaw.databinding.ActivityJobWelfareSelectorBinding
import com.shiyuan.fastclaw.feature.information.viewModel.JobWelfareSelectorModel

/**
 * @desc：福利偏好
 * @author：CaoWei
 * @date：2026/6/12 16:25
 */
class JobWelfareSelectorActivity : BaseViewModelActivity<ActivityJobWelfareSelectorBinding, JobWelfareSelectorModel>() {

    private val selectedWelfare = linkedMapOf<String, CommonEnumItem>()
    private var welfareItems = emptyList<CommonEnumItem>()
    private val maxSelectCount = 10
    private val sessionId by lazy { intent.getStringExtra(EXTRA_SESSION_ID).orEmpty() }
    private val conversationId by lazy { intent.getStringExtra(EXTRA_CONVERSATION_ID).orEmpty() }

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
     * 初始化页面点击事件、完成按钮状态，并请求福利偏好字典数据。
     */
    private fun initView() {
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
        binding.gridWelfare.removeAllViews()
        welfareItems.forEach { item ->
            binding.gridWelfare.addView(createWelfareChip(item))
        }
    }

    /**
     * 创建福利候选标签，并根据选中状态切换背景、文字颜色和字重。
     */
    private fun createWelfareChip(item: CommonEnumItem): TextView {
        val code = item.code.orEmpty()
        val selected = code.isNotBlank() && selectedWelfare.containsKey(code)
        return TextView(this).apply {
            text = item.name.orEmpty()
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (selected) R.color.color_2f6bff else R.color.color_90_000000
                )
            )
            textSize = 12f
            typeface = if (selected) {
                android.graphics.Typeface.DEFAULT_BOLD
            } else {
                android.graphics.Typeface.DEFAULT
            }
            background = ContextCompat.getDrawable(
                context,
                if (selected) R.drawable.bg_welfare_chip_selected else R.drawable.bg_welfare_chip
            )
            setPadding(8.dp, 0, 8.dp, 0)
            setOnSingleClickListener { toggleWelfare(item) }
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 36.dp
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(0, 0, 8.dp, 8.dp)
            }
        }
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
        renderWelfareGrid()
        refreshSelectedState()
    }

    /**
     * 从底部已选区域移除指定福利，并同步候选网格选中态。
     */
    private fun removeSelectedWelfare(item: CommonEnumItem) {
        val code = item.code
        if (code.isNullOrBlank()) return
        selectedWelfare.remove(code)
        renderWelfareGrid()
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
        binding.llSelectedChips.removeAllViews()
        selectedWelfare.values.forEach { item ->
            binding.llSelectedChips.addView(createSelectedChip(item))
        }
    }

    /**
     * 创建底部已选福利标签，点击标签或删除符号时取消选中。
     */
    private fun createSelectedChip(item: CommonEnumItem): LinearLayout {
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_welfare_selected_tag)
            setPadding(8.dp, 0, 6.dp, 0)
            isClickable = true
            setOnSingleClickListener { removeSelectedWelfare(item) }
            layoutParams = LinearLayout.LayoutParams(102.dp, 22.dp).apply {
                marginEnd = 8.dp
            }
            addView(TextView(context).apply {
                text = item.name.orEmpty()
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(ContextCompat.getColor(context, R.color.color_2f6bff))
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            })
            addView(TextView(context).apply {
                text = "×"
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(context, R.color.color_2f6bff))
                textSize = 12f
                setOnSingleClickListener { removeSelectedWelfare(item) }
                layoutParams = LinearLayout.LayoutParams(10.dp, ViewGroup.LayoutParams.MATCH_PARENT)
            })
        }
    }
}
