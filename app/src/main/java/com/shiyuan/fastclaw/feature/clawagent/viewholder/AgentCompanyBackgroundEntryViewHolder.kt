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

    var onCompanyStartClickListener: (item: ClawAgentMessageInfo, action: CardAction?, companyData: CompanyBackgroundStartBody) -> Unit = { _, _, _ -> }
    var onCompanySearchQueryListener: (keyword: String, callback: (keyword: String, companies: List<CompanyBackgroundCompanyInfo>) -> Unit, ) -> Unit = { keyword, callback -> callback(keyword, emptyList()) }

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var selectedCompanyId: Long? = null
    private var hasSetupListeners = false
    private var suppressSearch = false
    private var companySearchPopup: PopupWindow? = null

    fun bind(item: ClawAgentMessageInfo) {
        val payload = item.metaData?.let {
            runCatching { GsonUtils.fromJson(it, CompanyBackgroundEntryPayloadBean::class.java) }.getOrNull()
        }
        val action = payload?.actions?.firstOrNull()
        //val enabled = action != null && action.disabled != true && payload?.interactionState?.disabled != true
        /*binding.tvAction.isEnabled = enabled
        binding.tvAction.alpha = if (enabled) 1f else 0.5f*/

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
            scheduleCompanySearch(keyword)
        }
    }

    fun clear() {
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        searchRunnable = null
        dismissCompanySearchPopup()
    }

    private fun scheduleCompanySearch(keyword: String) {
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        if (keyword.isEmpty()) {
            dismissCompanySearchPopup()
            return
        }
        searchRunnable = Runnable {
            onCompanySearchQueryListener(keyword) { resultKeyword, companies ->
                val currentKeyword = binding.etSearch.text?.toString().orEmpty().trim()
                if (currentKeyword == resultKeyword) {
                    showCompanySearchPopup(resultKeyword, companies)
                }
            }
        }.also { searchHandler.postDelayed(it, SEARCH_DEBOUNCE_DELAY) }
    }

    private fun showCompanySearchPopup(keyword: String, companies: List<CompanyBackgroundCompanyInfo>) {
        val anchor = binding.llCompanyBackgroundInput
        if (anchor.width <= 0 || keyword.isEmpty()) return
        val content = LinearLayout(anchor.context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(context, R.color.color_ffffff))
                cornerRadius = SEARCH_POPUP_CORNER_RADIUS.toFloat()
            }
        }
        val companyItems = companies.filter { it.displayName().isNotBlank() }.take(MAX_SEARCH_RESULT_COUNT)
        if (companyItems.isEmpty()) {
            content.addView(createCompanySearchRow(TEXT_NO_COMPANY, keyword, enabled = false, showDivider = false))
        } else {
            companyItems.forEachIndexed { index, company ->
                content.addView(createCompanySearchRow(company, keyword, showDivider = index < companyItems.lastIndex))
            }
        }
        dismissCompanySearchPopup()
        companySearchPopup = PopupWindow(content, anchor.width, SEARCH_POPUP_HEIGHT, false).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = SEARCH_POPUP_ELEVATION.toFloat()
            showAsDropDown(anchor, 0, SEARCH_POPUP_OFFSET_Y)
        }
    }

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

    private fun createCompanySearchRow(company: CompanyBackgroundCompanyInfo, keyword: String, showDivider: Boolean, ): LinearLayout {
        return createCompanySearchRow(company.displayName(), keyword, enabled = true, showDivider = showDivider).apply {
            setOnSingleClickListener {
                val name = company.displayName()
                selectedCompanyId = company.companyId
                suppressSearch = true
                binding.etSearch.setText(name)
                binding.etSearch.setSelection(name.length)
                suppressSearch = false
                dismissCompanySearchPopup()
            }
        }
    }

    private fun buildCompanySearchText(name: String, keyword: String): SpannableString {
        val spannable = SpannableString(name)
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

    private fun dismissCompanySearchPopup() {
        companySearchPopup?.dismiss()
        companySearchPopup = null
    }

    companion object {
        private const val SEARCH_DEBOUNCE_DELAY = 120L
        private const val MAX_SEARCH_RESULT_COUNT = 4
        private const val TEXT_NO_COMPANY = "暂无相关公司"
        private val SEARCH_POPUP_HEIGHT = 177.dp
        private val SEARCH_ROW_HEIGHT = 44.dp
        private val SEARCH_POPUP_OFFSET_Y = 8.dp
        private val SEARCH_POPUP_ELEVATION = 4.dp
        private val SEARCH_POPUP_CORNER_RADIUS = 10.dp
        private val SEARCH_DIVIDER_HORIZONTAL_MARGIN = 23.dp
    }
}
