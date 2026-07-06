package com.shiyuan.fastclaw.feature.information

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.utils.KeyboardUtils.hideKeyboard
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumTreeUtil
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.ActivityIndustrySelectionBinding
import com.shiyuan.fastclaw.feature.information.adapter.IyPnAdapter
import com.shiyuan.fastclaw.feature.information.adapter.IyPnSonAdapter
import com.shiyuan.fastclaw.feature.information.adapter.SearchAdapter
import com.shiyuan.fastclaw.feature.information.adapter.SelectedIyPnAdapter
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel


/**
 * 行业选择页：左侧一级行业、右侧分组网格展示所有二级行业，
 * 支持单选（搜索 / 列表点击直接回传）和多选（底部已选 + 完成回调）。
 */
class IndustrySelectionActivity :
    BaseViewModelActivity<ActivityIndustrySelectionBinding, InformationViewModel>() {
    companion object {
        // 是否多选模式参数 key
        private const val EXTRA_IS_MULTIPLE = "extra_is_multiple"
        // 会话 ID / 对话 ID
        private const val SESSIONID = "SESSIONID"
        private const val CONVERSATIONID = "CONVERSATIONID"
        private const val RIGHT_GRID_SPAN_COUNT = 2
        private const val MAX_SELECTED_INDUSTRY_COUNT = 3
        private const val INDUSTRY_GRID_PLACEHOLDER_CODE = "__industry_grid_placeholder__"

        /**
         * 启动行业选择页。
         */
        fun start(
            activity: Activity?,
            isMultiple: Boolean = false,
            sessionId: String? = "",
            conversationId: String? = ""
        ) {
            activity ?: return
            val intent = Intent(activity, IndustrySelectionActivity::class.java).apply {
                putExtra(EXTRA_IS_MULTIPLE, isMultiple)
                putExtra(SESSIONID, sessionId)
                putExtra(CONVERSATIONID, conversationId)
            }
            activity.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化视图与点击事件
        initView()
        // 监听 ViewModel 状态变化
        observeEvents()
        // 首次进入请求行业字典
        mRealVM.queryDictIndustryQuery(sessionId, conversationId)
    }


    // 是否多选模式
    private val isMultiple: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_MULTIPLE, false) }
    // 会话 ID / 对话 ID
    private val sessionId: String by lazy { intent.getStringExtra(SESSIONID) ?: "" }
    private val conversationId: String by lazy { intent.getStringExtra(CONVERSATIONID) ?: "" }
    private val rightIndustryItems = mutableListOf<CommonEnumItem>()
    private val rightSections = mutableListOf<IndustrySection>()
    private val rightGroupDecoration by lazy {
        StickyHeaderDecoration(
            context = this,
            headerLayoutId = R.layout.item_industry_group_header,
            titleViewId = R.id.tv_name,
            spanCount = RIGHT_GRID_SPAN_COUNT,
            reserveHeaderSpace = true
        )
    }
    private val leftLayoutManager: LinearLayoutManager?
        get() = binding.rcLeft.layoutManager as? LinearLayoutManager
    private val rightLayoutManager: GridLayoutManager?
        get() = binding.rcRight.layoutManager as? GridLayoutManager
    private var suppressRightScrollSync = false
    private var isViewDestroyed = false

    // 左侧一级行业列表适配器
    private val cyIyPnAdapter: IyPnAdapter by lazy {
        IyPnAdapter({ _, position -> onLeftIndustryClick(position) }, 0)
    }

    // 右侧二级行业网格适配器
    private val cyIyPnSonAdapter: IyPnSonAdapter by lazy {
        IyPnSonAdapter({ item, _ -> onRightIndustryClick(item) }, 0, ::isIndustryGridPlaceholder)
    }

    // 底部已选行业横滑标签适配器
    private val selectCyIyPnAdapter: SelectedIyPnAdapter by lazy {
        SelectedIyPnAdapter({ item -> removeSelectedIndustry(item) }, 0)
    }

    // 搜索结果列表适配器
    private val searchAdapter: SearchAdapter by lazy {
        SearchAdapter({ item, _ -> onSearchIndustryClick(item) }, 0) {
            binding.et.text?.toString().orEmpty()
        }
    }

    private fun initView() {
        initMultipleMode()
        initRecyclerViews()
        initSearch()
        initClicks()
    }

    private fun initMultipleMode() {
        if (isMultiple) {
            binding.tvTitle.text = getString(R.string.choose_expected_industry)
            binding.llMultipleTop.visibility = View.VISIBLE
            binding.llMultipleBot.visibility = View.VISIBLE
            binding.rcSelected.adapter = selectCyIyPnAdapter
        } else {
            binding.tvTitle.text = getString(R.string.industry)
            binding.llMultipleTop.visibility = View.GONE
            binding.llMultipleBot.visibility = View.GONE
        }
    }

    private fun initRecyclerViews() {
        binding.rcLeft.adapter = cyIyPnAdapter

        binding.rcRight.apply {
            addItemDecoration(rightGroupDecoration)
            addOnScrollListener(rightScrollListener)
            addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                        suppressRightScrollSync = false
                    }
                    return false
                }
            })
            adapter = cyIyPnSonAdapter
        }

        binding.rcSc.adapter = searchAdapter
    }

    private fun initSearch() {
        binding.et.doAfterTextChanged { editable ->
            val keyword = editable?.toString().orEmpty()
            if (keyword.isNotEmpty()) {
                mRealVM.queryDictIndustryQuery(keyword, sessionId, conversationId)
                binding.ivClear.visibility = View.VISIBLE
                binding.rlSc.visibility = View.VISIBLE
            } else {
                binding.rlSc.visibility = View.GONE
                binding.ivClear.visibility = View.GONE
            }
        }

        binding.ivClear.setOnSingleClickListener {
            closeSearchPanel(shouldHideKeyboard = false)
            binding.et.text.clear()
        }
    }

    private fun initClicks() {
        binding.ivBack.setOnSingleClickListener { finish() }

        binding.layoutPrimary.setOnSingleClickListener {
            val selectedList = mRealVM.selectData.value?.toMutableList() ?: mutableListOf()
            if (selectedList.isEmpty()) {
                "最少选择一个行业".showToast()
                return@setOnSingleClickListener
            }
            val msgGson = CommonEnumTreeUtil.toJson(selectedList)
            FlowEventBus.postSticky(Events.ClawAgentEvent(msgGson))
            finish()
        }
    }

    private fun onLeftIndustryClick(position: Int) {
        selectLeftIndustry(position, scrollLeft = false)
        scrollRightToIndustry(position, smooth = false, suppressSync = true)
    }

    private fun onRightIndustryClick(item: CommonEnumItem) {
        if (isIndustryGridPlaceholder(item)) return
        if (isMultiple) {
            toggleIndustrySelection(item)
        } else {
            finishWithIndustry(item)
        }
        cyIyPnSonAdapter.notifyDataSetChanged()
    }

    private fun onSearchIndustryClick(item: CommonEnumItem) {
        if (isMultiple) {
            selectIndustryFromSearch(item)
        } else {
            finishWithIndustry(item)
        }
    }

    private fun toggleIndustrySelection(item: CommonEnumItem) {
        val selectedList = mRealVM.selectData.value?.toMutableList() ?: mutableListOf()
        if (item.isSelected) {
            item.isSelected = false
            mRealVM.selectData.value = selectedList.filterNot { it.code == item.code }
            return
        }

        if (selectedList.size >= MAX_SELECTED_INDUSTRY_COUNT) {
            "最多选择三个".showToast()
            return
        }
        item.isSelected = true
        mRealVM.selectData.value = (selectedList + item).distinctBy { it.code }
    }

    private fun removeSelectedIndustry(item: CommonEnumItem) {
        mRealVM.selectData.value = mRealVM.selectData.value?.filterNot { it.code == item.code }
        setChildSelected(item.code, false)
        cyIyPnSonAdapter.notifyDataSetChanged()
    }

    private fun selectIndustryFromSearch(item: CommonEnumItem) {
        val selectedList = mRealVM.selectData.value?.toMutableList() ?: mutableListOf()
        val alreadySelected = selectedList.any { it.code == item.code }
        if (!alreadySelected && selectedList.size >= MAX_SELECTED_INDUSTRY_COUNT) {
            "最多选择三个".showToast()
            return
        }

        item.isSelected = true
        setChildSelected(item.code, true)
        mRealVM.selectData.value = (selectedList + item).distinctBy { it.code }
        cyIyPnSonAdapter.notifyDataSetChanged()
        closeSearchPanel()
        locateSelectedIndustry(item)
    }

    private fun setChildSelected(code: String?, isSelected: Boolean) {
        if (code.isNullOrEmpty()) return
        mRealVM.allData.value?.forEach { parent ->
            parent.children?.firstOrNull { child -> child.code == code }?.isSelected = isSelected
        }
    }

    private fun closeSearchPanel(shouldHideKeyboard: Boolean = true) {
        binding.rlSc.visibility = View.GONE
        if (shouldHideKeyboard) {
            hideKeyboard(binding.et)
        }
    }

    private fun finishWithIndustry(item: CommonEnumItem) {
        item.fieldCode = "industry"
        FlowEventBus.postSticky(Events.CommonEnumItemEvent(item))
        finish()
    }

    private fun observeEvents() {
        observeSelectedData()
        observeSearchData()
        observeIndustryData()
    }

    private fun observeSelectedData() {
        mRealVM.selectData.observe(this) {
            binding.tvWordCount.text = it.size.toString()
            selectCyIyPnAdapter.submitList(it)
        }
    }

    private fun observeSearchData() {
        mRealVM.searchData.observe(this) {
            showSearchEmpty(it.isNullOrEmpty())
            searchAdapter.submitList(it)
        }
    }

    private fun observeIndustryData() {
        mRealVM.allData.observe(this) {
            cyIyPnAdapter.submitList(it)
            submitRightIndustryList(it)
            val targetPosition = (mRealVM.lastSelectedPosition.value ?: 1)
                .takeIf { position -> position in it.indices }
                ?: 0
            selectLeftIndustry(targetPosition, scrollLeft = false)
            binding.rcRight.post {
                if (!isViewDestroyed) {
                    scrollRightToIndustry(targetPosition, smooth = false)
                }
            }
        }
    }

    private fun showSearchEmpty(isEmpty: Boolean) {
        binding.tvEm.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rcSc.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun setTranslucentStatus() {
        // 设置沉浸式状态栏
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

    private val rightScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (isViewDestroyed || suppressRightScrollSync) return
            syncLeftByRightPosition(recyclerView)
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (isViewDestroyed) return
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (suppressRightScrollSync) {
                    suppressRightScrollSync = false
                    return
                }
                syncLeftByRightPosition(recyclerView)
            }
        }
    }

    private fun submitRightIndustryList(data: List<CommonEnumItem>) {
        rightIndustryItems.clear()
        rightSections.clear()

        val groups = buildIndustryGroups(data)
        groups.forEachIndexed { groupIndex, group ->
            appendIndustryGroup(group, hasNextGroup = groupIndex < groups.lastIndex)
        }

        rightGroupDecoration.submitSections(rightSections.map { it.toStickyHeaderSection() })
        cyIyPnSonAdapter.submitList(rightIndustryItems.toList())
        binding.rcRight.post {
            if (!isViewDestroyed) {
                binding.rcRight.invalidateItemDecorations()
            }
        }
    }

    private fun buildIndustryGroups(data: List<CommonEnumItem>): List<IndustryGroup> {
        return data.mapIndexedNotNull { leftIndex, parent ->
            val children = parent.children.orEmpty().takeIf { it.isNotEmpty() } ?: return@mapIndexedNotNull null
            IndustryGroup(
                leftIndex = leftIndex,
                title = parent.name.orEmpty().ifBlank { parent.fullNamePath.orEmpty() },
                children = children
            )
        }
    }

    private fun appendIndustryGroup(group: IndustryGroup, hasNextGroup: Boolean) {
        rightSections.add(
            IndustrySection(
                leftIndex = group.leftIndex,
                startPosition = rightIndustryItems.size,
                title = group.title
            )
        )
        rightIndustryItems.addAll(group.children)
        if (hasNextGroup) {
            appendGridPlaceholders(group.children.size)
        }
    }

    private fun appendGridPlaceholders(itemCount: Int) {
        val placeholderCount = (RIGHT_GRID_SPAN_COUNT - itemCount % RIGHT_GRID_SPAN_COUNT) % RIGHT_GRID_SPAN_COUNT
        repeat(placeholderCount) {
            rightIndustryItems.add(CommonEnumItem(code = INDUSTRY_GRID_PLACEHOLDER_CODE))
        }
    }

    private fun selectLeftIndustry(position: Int, scrollLeft: Boolean = true) {
        if (isViewDestroyed) return
        val data = mRealVM.allData.value ?: return
        if (position !in data.indices) return
        val oldPosition = mRealVM.lastSelectedPosition.value
        val alreadySelected = data.getOrNull(position)?.isSelected == true
        if (oldPosition == position && alreadySelected) return
        data.forEachIndexed { index, item -> item.isSelected = index == position }
        mRealVM.lastSelectedPosition.value = position
        cyIyPnAdapter.notifyDataSetChanged()
        if (scrollLeft) {
            leftLayoutManager?.scrollToPositionWithOffset(position, 0)
        }
    }

    private fun locateSelectedIndustry(item: CommonEnumItem) {
        if (isViewDestroyed) return
        val targetCode = item.code ?: return
        val rightPosition = rightIndustryItems.indexOfFirst { child ->
            !isIndustryGridPlaceholder(child) && child.code == targetCode
        }
        if (rightPosition < 0) return
        val leftIndex = rightSections.lastOrNull { it.startPosition <= rightPosition }?.leftIndex ?: return
        selectLeftIndustry(leftIndex, scrollLeft = false)
        scrollLeftToIndustry(leftIndex)
        binding.rcRight.post {
            if (!isViewDestroyed) {
                scrollRightToIndustryItem(rightPosition, suppressSync = true)
            }
        }
    }

    private fun scrollLeftToIndustry(position: Int) {
        if (isViewDestroyed) return
        leftLayoutManager?.scrollToPositionWithOffset(position, 0)
    }

    private fun scrollRightToIndustry(
        leftIndex: Int,
        smooth: Boolean = true,
        suppressSync: Boolean = false
    ) {
        if (isViewDestroyed) return
        if (suppressSync) {
            suppressRightScrollSync = true
        }
        val section = rightSections.firstOrNull { it.leftIndex == leftIndex } ?: return
        if (smooth) {
            val smoothScroller = object : LinearSmoothScroller(this) {
                override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            }
            smoothScroller.targetPosition = section.startPosition
            rightLayoutManager?.startSmoothScroll(smoothScroller)
        } else {
            rightLayoutManager?.scrollToPositionWithOffset(section.startPosition, 0)
        }
    }

    private fun scrollRightToIndustryItem(rightPosition: Int, suppressSync: Boolean = false) {
        if (isViewDestroyed) return
        if (suppressSync) {
            suppressRightScrollSync = true
        }
        val section = rightSections.lastOrNull { it.startPosition <= rightPosition } ?: return
        val offset = if (rightPosition < section.startPosition + RIGHT_GRID_SPAN_COUNT) {
            0
        } else {
            rightGroupDecoration.headerHeight
        }
        rightLayoutManager?.scrollToPositionWithOffset(rightPosition, offset)
    }

    private fun syncLeftByRightPosition(recyclerView: RecyclerView) {
        if (isViewDestroyed) return
        if (rightSections.isEmpty()) return
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION) return
        val section = rightSections.lastOrNull { it.startPosition <= firstVisible } ?: return
        selectLeftIndustry(section.leftIndex)
    }

    private fun isIndustryGridPlaceholder(item: CommonEnumItem): Boolean {
        return item.code == INDUSTRY_GRID_PLACEHOLDER_CODE
    }

    override fun onDestroy() {
        isViewDestroyed = true
        runCatching {
            binding.rcRight.removeOnScrollListener(rightScrollListener)
        }
        super.onDestroy()
    }

    private data class IndustryGroup(
        val leftIndex: Int,
        val title: String,
        val children: List<CommonEnumItem>
    )

    private data class IndustrySection(
        val leftIndex: Int,
        val startPosition: Int,
        val title: String
    ) {
        fun toStickyHeaderSection(): StickyHeaderSection {
            return StickyHeaderSection(startPosition, title)
        }
    }

}
