package com.shiyuan.fastclaw.feature.clawagent.viewholder

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.afterTextChanged
import com.shiyuan.fastclaw.core.network.apis.request.CompanyBackgroundStartBody
import com.shiyuan.fastclaw.core.network.apis.response.CardAction
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundCompanyInfo
import com.shiyuan.fastclaw.core.network.apis.response.CompanyBackgroundEntryPayloadBean
import com.shiyuan.fastclaw.databinding.ItemMsgAgentCompanyBackgroundEntryBinding
import com.shiyuan.fastclaw.feature.clawagent.bean.ClawAgentMessageInfo

/**
 * 公司背调入口卡
 */
class AgentCompanyBackgroundEntryViewHolder(private val binding: ItemMsgAgentCompanyBackgroundEntryBinding, ) : RecyclerView.ViewHolder(binding.root) {

    /** 点击“开始背调”时回调给 Fragment，由外层提交卡片动作。 */
    var onCompanyStartClickListener: (item: ClawAgentMessageInfo, action: CardAction?, companyData: CompanyBackgroundStartBody) -> Unit = { _, _, _ -> }

    /** 输入公司名后的搜索回调，由外层请求候选公司列表后通过 callback 回传。 */
    var onCompanySearchQueryListener: (keyword: String, callback: (keyword: String, companies: List<CompanyBackgroundCompanyInfo>) -> Unit, ) -> Unit = { keyword, callback -> callback(keyword, emptyList()) }

    /** 搜索输入防抖用 Handler，避免每次输入都立刻请求接口。 */
    private val searchHandler = Handler(Looper.getMainLooper())

    /** 当前等待执行的搜索任务，输入变化时会取消旧任务。 */
    private var searchRunnable: Runnable? = null

    /** 用户从候选列表中选中的公司 ID，提交时必须带给服务端。 */
    private var selectedCompanyId: Long? = null

    /** ViewHolder 复用时避免重复绑定点击和文本监听。 */
    private var hasSetupListeners = false

    /** 选中候选公司后回填输入框时，临时禁止触发搜索。 */
    private var suppressSearch = false

    /** 公司搜索候选弹窗，输入变化或 item 回收时需要关闭。 */
    private var companySearchPopup: PopupWindow? = null

    /** 绑定公司背调入口卡数据，并在首次绑定时初始化交互事件。 */
    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CompanyBackgroundEntryPayloadBean::class.java) }.getOrNull()
        }
        val action = payload?.actions?.firstOrNull()
        //val enabled = action != null && action.disabled != true && payload?.interactionState?.disabled != true
        /*binding.tvAction.isEnabled = enabled
        binding.tvAction.alpha = if (enabled) 1f else 0.5f*/

        // RecyclerView 复用会多次 bind，同一个 View 只需要设置一次监听。
        if (hasSetupListeners) return
        hasSetupListeners = true
        binding.layoutAction.setOnSingleClickListener {
            val companyName = binding.etSearch.text?.toString().orEmpty().trim()
            if (companyName.isEmpty()) {
                showToast(R.string.str_agent_company_background_input_empty)
                return@setOnSingleClickListener
            }
            val companyId = selectedCompanyId
            if (companyId == null) {
                showToast(R.string.str_agent_company_background_company_unselected)
                return@setOnSingleClickListener
            }
            dismissCompanySearchPopup()
            binding.etSearch.setText("")
            selectedCompanyId = null
            // 只有输入内容来自候选公司时才允许发起背调，避免把未确认文本提交成公司 ID。
            onCompanyStartClickListener(item, action, CompanyBackgroundStartBody(companyName, companyId))
        }

        binding.ivDelete.setOnSingleClickListener {
            dismissCompanySearchPopup()
            selectedCompanyId = null
            binding.etSearch.setText("")
        }

        binding.etSearch.afterTextChanged {
            val keyword = it?.toString().orEmpty().trim()
            binding.ivDelete.visibility = if (keyword.isEmpty()) GONE else VISIBLE
            if (suppressSearch) return@afterTextChanged
            selectedCompanyId = null
            // 用户继续编辑后，之前选中的公司 ID 失效，需要重新搜索并选择。
            scheduleCompanySearch(keyword)
        }
    }

    /** item 滑出或回收时取消未执行的搜索任务，并关闭候选弹窗。 */
    fun clear() {
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        searchRunnable = null
        dismissCompanySearchPopup()
    }

    /** 延迟触发公司搜索；若用户继续输入，会先取消上一次待执行任务。 */
    private fun scheduleCompanySearch(keyword: String) {
        // 每次输入变化都取消旧任务，只保留最后一次输入触发搜索。
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        if (keyword.isEmpty()) {
            // 输入清空时不请求接口，同时关闭已展示的候选弹窗。
            dismissCompanySearchPopup()
            return
        }
        searchRunnable = Runnable {
            // 真正的搜索请求交给外层处理，ViewHolder 只负责收集关键字和展示结果。
            onCompanySearchQueryListener(keyword) { resultKeyword, companies ->
                val currentKeyword = binding.etSearch.text?.toString().orEmpty().trim()
                if (currentKeyword == resultKeyword) {
                    // 只展示与当前输入一致的结果，过滤掉慢请求回来的旧关键字数据。
                    showCompanySearchPopup(resultKeyword, companies)
                }
            }
        }.also { searchHandler.postDelayed(it, SEARCH_DEBOUNCE_DELAY) }
    }

    /** 在输入框下方展示公司候选列表，最多展示固定数量结果。 */
    private fun showCompanySearchPopup(keyword: String, companies: List<CompanyBackgroundCompanyInfo>) {
        val anchor = binding.llCompanyBackgroundInput
        // 输入框还没完成测量或关键字为空时，无法正确定位弹窗。
        if (anchor.width <= 0 || keyword.isEmpty()) return
        val content = LinearLayout(anchor.context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(context, R.color.color_ffffff))
                cornerRadius = SEARCH_POPUP_CORNER_RADIUS.toFloat()
            }
        }
        // 过滤空公司名并限制展示数量，避免弹窗过高。
        val companyItems = companies.filter { it.displayName().isNotBlank() }.take(MAX_SEARCH_RESULT_COUNT)
        if (companyItems.isEmpty()) {
            // 无结果时展示不可点击占位行。
            content.addView(createCompanySearchRow(TEXT_NO_COMPANY, keyword, enabled = false, showDivider = false))
        } else {
            companyItems.forEachIndexed { index, company ->
                // 最后一行不画分割线，避免弹窗底部多一条线。
                content.addView(createCompanySearchRow(company, keyword, showDivider = index < companyItems.lastIndex))
            }
        }
        // 新结果展示前先关闭旧弹窗，避免多个 PopupWindow 叠加。
        dismissCompanySearchPopup()
        companySearchPopup = PopupWindow(content, anchor.width, SEARCH_POPUP_HEIGHT, false).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = SEARCH_POPUP_ELEVATION.toFloat()
            showAsDropDown(anchor, 0, SEARCH_POPUP_OFFSET_Y)
        }
    }

    /** 创建候选列表行；enabled=false 时用于展示“暂无相关公司”。 */
    private fun createCompanySearchRow(name: String, keyword: String, enabled: Boolean, showDivider: Boolean, ): LinearLayout {
        val context = binding.root.context
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                SEARCH_ROW_HEIGHT
            )
            val textView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                gravity = android.view.Gravity.CENTER
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, if (enabled) R.color.color_96_000000 else R.color.color_55_3c3c43))
                // 可点击候选行高亮关键字；占位行直接展示普通文本。
                text = if (enabled) buildCompanySearchText(name, keyword) else name
            }
            addView(textView)
            if (showDivider) {
                addView(android.view.View(context).apply {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.color_10_000000))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1
                    ).apply {
                        marginStart = SEARCH_DIVIDER_HORIZONTAL_MARGIN
                        marginEnd = SEARCH_DIVIDER_HORIZONTAL_MARGIN
                    }
                })
            }
        }
    }

    /** 创建可点击的公司候选行，点击后回填公司名并记录公司 ID。 */
    private fun createCompanySearchRow(company: CompanyBackgroundCompanyInfo, keyword: String, showDivider: Boolean, ): LinearLayout {
        return createCompanySearchRow(company.displayName(), keyword, enabled = true, showDivider = showDivider).apply {
            setOnSingleClickListener {
                val name = company.displayName()
                selectedCompanyId = company.companyId
                suppressSearch = true
                // 回填候选公司名时禁止触发 afterTextChanged 搜索，避免弹窗刚选中又重新打开。
                binding.etSearch.setText(name)
                binding.etSearch.setSelection(name.length)
                suppressSearch = false
                dismissCompanySearchPopup()
            }
        }
    }

    /** 高亮候选公司名中命中的关键字。 */
    private fun buildCompanySearchText(name: String, keyword: String): SpannableString {
        val spannable = SpannableString(name)
        // 忽略大小写查找关键字位置，只高亮第一段命中的文本。
        val start = name.indexOf(keyword, ignoreCase = true)
        if (start >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(binding.root.context, R.color.color_2f6bff)),
                start,
                start + keyword.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    /** 关闭公司候选弹窗并释放引用，防止 ViewHolder 复用后残留。 */
    private fun dismissCompanySearchPopup() {
        companySearchPopup?.dismiss()
        companySearchPopup = null
    }

    companion object {
        /** 公司搜索输入防抖间隔，单位毫秒。 */
        private const val SEARCH_DEBOUNCE_DELAY = 120L

        /** 公司搜索弹窗最多展示的候选数量。 */
        private const val MAX_SEARCH_RESULT_COUNT = 4

        /** 没有搜索结果时展示的占位文案。 */
        private const val TEXT_NO_COMPANY = "暂无相关公司"

        /** 公司搜索弹窗固定高度，单位 dp。 */
        private val SEARCH_POPUP_HEIGHT = 177.dp

        /** 公司搜索候选行固定高度，单位 dp。 */
        private val SEARCH_ROW_HEIGHT = 44.dp

        /** 公司搜索弹窗相对输入框底部的下偏移，单位 dp。 */
        private val SEARCH_POPUP_OFFSET_Y = 8.dp

        /** 公司搜索弹窗阴影高度，单位 dp。 */
        private val SEARCH_POPUP_ELEVATION = 4.dp

        /** 公司搜索弹窗圆角半径，单位 dp。 */
        private val SEARCH_POPUP_CORNER_RADIUS = 10.dp

        /** 候选行分割线左右边距，单位 dp。 */
        private val SEARCH_DIVIDER_HORIZONTAL_MARGIN = 23.dp
    }
}
