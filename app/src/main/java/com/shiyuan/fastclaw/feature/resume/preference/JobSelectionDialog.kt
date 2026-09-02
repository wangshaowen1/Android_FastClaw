package com.shiyuan.fastclaw.feature.resume.preference

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.BaseBottomDialogVmFragment
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.common.utils.KeyboardUtils.hideKeyboard
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.DialogJobSelectionBinding
import com.shiyuan.fastclaw.feature.resume.InformationViewModel

/**
 * 职位选择底部弹窗：复用职位选择页的单选、多选、搜索与完成回传逻辑。
 */
class JobSelectionDialog :
    BaseBottomDialogVmFragment<DialogJobSelectionBinding, InformationViewModel>() {

    companion object {
        const val TAG = "JobSelectionDialog"

        private const val ARG_IS_MULTIPLE = "arg_is_multiple"
        private const val ARG_SESSION_ID = "arg_session_id"
        private const val ARG_CONVERSATION_ID = "arg_conversation_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MULTIPLE_NUM = "arg_multiple_num"
        private const val ARG_TYPE = "arg_type"
        private const val JOB_RIGHT_SPAN_COUNT = 2
        private const val JOB_SECTION_HEADER_CODE = "__job_section_header__"

        fun show(
            manager: FragmentManager,
            isMultiple: Boolean = false,
            sessionId: String? = "",
            conversationId: String? = "",
            title: String? = null,
            multipleNum: Int = 3,
            type: String = "ClawAgent"
        ): JobSelectionDialog {
            return JobSelectionDialog().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_MULTIPLE, isMultiple)
                    putString(ARG_SESSION_ID, sessionId)
                    putString(ARG_CONVERSATION_ID, conversationId)
                    putString(ARG_TITLE, title)
                    putInt(ARG_MULTIPLE_NUM, multipleNum)
                    putString(ARG_TYPE, type)
                }
                show(manager)
            }
        }
    }

    private val title: String by lazy { arguments?.getString(ARG_TITLE).orEmpty() }
    private val type: String by lazy { arguments?.getString(ARG_TYPE) ?: "ClawAgent" }
    private val multipleNum: Int by lazy { arguments?.getInt(ARG_MULTIPLE_NUM, 3) ?: 3 }
    private val isMultiple: Boolean by lazy { arguments?.getBoolean(ARG_IS_MULTIPLE, false) ?: false }
    private val sessionId: String by lazy { arguments?.getString(ARG_SESSION_ID).orEmpty() }
    private val conversationId: String by lazy { arguments?.getString(ARG_CONVERSATION_ID).orEmpty() }

    private val rightJobCategoryItems = mutableListOf<CommonEnumItem>()
    private val rightJobSections = mutableListOf<JobSection>()
    private val rightGroupDecoration by lazy {
        StickyHeaderDecoration(
            context = requireContext(),
            headerLayoutId = R.layout.item_job_second_category_header,
            titleViewId = R.id.tv_name
        )
    }
    private val leftLayoutManager: LinearLayoutManager?
        get() = binding.rcLeft.layoutManager as? LinearLayoutManager
    private val rightLayoutManager: GridLayoutManager?
        get() = binding.rcRight.layoutManager as? GridLayoutManager
    private var suppressRightScrollSync = false
    private var isViewDestroyed = false

    private val cyIyPnAdapter: IyPnAdapter by lazy {
        IyPnAdapter({ _, position -> onLeftJobClick(position) }, 1)
    }

    private val jobRightAdapter: JobRightAdapter by lazy {
        JobRightAdapter(::isJobSectionHeader) { item, _ -> onRightJobClick(item) }
    }

    private val selectCyIyPnAdapter: SelectedIyPnAdapter by lazy {
        SelectedIyPnAdapter({ item -> removeSelectedJob(item) }, 1)
    }

    private val searchAdapter: SearchAdapter by lazy {
        SearchAdapter({ item, _ ->
            onSearchJobClick(item)
        }, 1) { binding.et.text?.toString().orEmpty() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialogWindow.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.85f).toInt()
            )
            val attr = dialogWindow.attributes
            attr.gravity = Gravity.BOTTOM
            dialogWindow.attributes = attr
        }
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        isViewDestroyed = false
        initView()
        observeViewModel()
        mRealVM.queryDictPositionQuery(sessionId, conversationId)
    }

    private fun selectOnlyPosition(
        item: CommonEnumItem,
        locateAfterSelect: Boolean = true
    ) {
        val selectedCode = item.positionCode
        var selectedParentIndex: Int? = null
        mRealVM.allPositioData.value?.forEachIndexed { parentIndex, parent ->
            var parentSelected = false
            parent.secondCategories?.forEach { secondCategory ->
                var secondCategorySelected = false
                secondCategory.positions?.forEach { child ->
                    val childSelected = selectedCode != null && child.positionCode == selectedCode
                    child.isSelected = childSelected
                    if (childSelected) {
                        secondCategorySelected = true
                        parentSelected = true
                    }
                }
                secondCategory.isSelected = secondCategorySelected
            }
            parent.isSelected = parentSelected
            if (parentSelected) {
                selectedParentIndex = parentIndex
            }
        }
        item.isSelected = true
        mRealVM.selectPositionData.value = listOf(item)
        selectedParentIndex?.let { leftIndex ->
            selectLeftJob(leftIndex, scrollLeft = false)
            scrollLeftToJob(leftIndex)
        }
        jobRightAdapter.notifyDataSetChanged()
        if (locateAfterSelect) {
            locateSelectedJob(item)
        }
    }

    private fun onLeftJobClick(position: Int) {
        selectLeftJob(position, scrollLeft = false)
        scrollRightToJob(position, smooth = false, suppressSync = true)
    }

    private fun onRightJobClick(item: CommonEnumItem) {
        if (isJobSectionHeader(item)) return
        if (isMultiple) {
            toggleJobSelection(item)
        } else {
            finishWithJob(item)
        }
        jobRightAdapter.notifyDataSetChanged()
    }

    private fun onSearchJobClick(item: CommonEnumItem) {
        if (isMultiple) {
            selectJobFromSearch(item)
        } else {
            finishWithJob(item)
        }
    }

    private fun toggleJobSelection(item: CommonEnumItem) {
        val selectedList = mRealVM.selectPositionData.value?.toMutableList() ?: mutableListOf()
        if (item.isSelected) {
            item.isSelected = false
            mRealVM.selectPositionData.value = selectedList.filterNot { it.positionCode == item.positionCode }
            return
        }

        if (multipleNum == 1) {
            selectOnlyPosition(item, locateAfterSelect = false)
            return
        }

        if (selectedList.size >= multipleNum) {
            "最多选择${multipleNum}个".showToast()
            return
        }
        item.isSelected = true
        mRealVM.selectPositionData.value = (selectedList + item).distinctBy { it.positionCode }
    }

    private fun removeSelectedJob(item: CommonEnumItem) {
        mRealVM.selectPositionData.value =
            mRealVM.selectPositionData.value?.filterNot { it.positionCode == item.positionCode }
        setJobSelected(item.positionCode, false)
        jobRightAdapter.notifyDataSetChanged()
    }

    private fun selectJobFromSearch(item: CommonEnumItem) {
        if (multipleNum == 1) {
            selectOnlyPosition(item)
            closeSearchPanel()
            return
        }

        val selectedList = mRealVM.selectPositionData.value?.toMutableList() ?: mutableListOf()
        val alreadySelected = selectedList.any { it.positionCode == item.positionCode }
        if (!alreadySelected && selectedList.size >= multipleNum) {
            "最多选择${multipleNum}个".showToast()
            return
        }

        item.isSelected = true
        setJobSelected(item.positionCode, true)
        mRealVM.selectPositionData.value = (selectedList + item).distinctBy { it.positionCode }
        jobRightAdapter.notifyDataSetChanged()
        closeSearchPanel()
        locateSelectedJob(item)
    }

    private fun setJobSelected(positionCode: String?, isSelected: Boolean) {
        if (positionCode.isNullOrEmpty()) return
        mRealVM.allPositioData.value?.forEach { parent ->
            parent.secondCategories?.forEach { secondCategory ->
                secondCategory.positions
                    ?.firstOrNull { child -> child.positionCode == positionCode }
                    ?.isSelected = isSelected
            }
        }
    }

    private fun closeSearchPanel(shouldHideKeyboard: Boolean = true) {
        binding.rlSc.visibility = View.GONE
        if (shouldHideKeyboard) {
            hideKeyboard(binding.et)
        }
    }

    private fun finishWithJob(item: CommonEnumItem) {
        item.fieldCode = "position"
        FlowEventBus.postSticky(Events.CommonEnumItemEvent(item))
        dismissAllowingStateLoss()
    }

    private fun initView() {
        initMultipleMode()
        initRecyclerViews()
        initSearch()
        initClicks()
    }

    private fun initMultipleMode() {
        if (isMultiple) {
            binding.tvTitle.text = getString(R.string.choose_desired_position)
            binding.llMultipleTop.visibility = View.GONE
            binding.llMultipleBot.visibility = View.VISIBLE
            binding.tvAllCount.text = "/$multipleNum"
            binding.rcSelected.adapter = selectCyIyPnAdapter
        } else {
            binding.tvTitle.text = getString(R.string.select_job_title)
            binding.llMultipleTop.visibility = View.GONE
            binding.llMultipleBot.visibility = View.GONE
        }

        if (title.isNotEmpty()) {
            binding.tvTitle.text = title
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
            setHasFixedSize(true)
            itemAnimator = null
            rightLayoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (jobRightAdapter.isHeaderPosition(position)) JOB_RIGHT_SPAN_COUNT else 1
                }
            }
            adapter = jobRightAdapter
        }

        binding.rcSc.adapter = searchAdapter
    }

    private fun initSearch() {
        binding.et.doAfterTextChanged { editable ->
            val keyword = editable?.toString().orEmpty()
            if (keyword.isNotEmpty()) {
                mRealVM.queryDictPositionQuery(keyword, sessionId, conversationId)
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
        binding.ivBack.setOnSingleClickListener { dismissAllowingStateLoss() }

        binding.layoutPrimary.setOnSingleClickListener {
            val selectedList = mRealVM.selectPositionData.value?.toMutableList() ?: mutableListOf()
            if (selectedList.isEmpty()) {
                "最少选择一个岗位".showToast()
                return@setOnSingleClickListener
            }
            val categoryList = mRealVM.selectPositionData.value?.map {
                mapOf("code" to it.positionCode, "name" to it.positionName)
            }
            if (type == "Interview") {
                FlowEventBus.postSticky(Events.InterviewEvent(categoryList?.toJsonString()))
            } else {
                FlowEventBus.postSticky(
                    Events.ClawAgentEvent(
                        categoryList?.toJsonString(),
                        AppEvent.KLX_C_EXPECTED_JOB_COMPLETED
                    )
                )
            }
            dismissAllowingStateLoss()
        }
    }

    private fun observeViewModel() {
        observeSelectedJobs()
        observeSearchJobs()
        observeJobData()
    }

    private fun observeSelectedJobs() {
        mRealVM.selectPositionData.observe(viewLifecycleOwner) {
            val selectedJobs = it.orEmpty()
            binding.tvWordCount.text = selectedJobs.size.toString()
            selectCyIyPnAdapter.submitList(selectedJobs)
            jobRightAdapter.notifyDataSetChanged()
        }
    }

    private fun observeSearchJobs() {
        mRealVM.searchPositionData.observe(viewLifecycleOwner) {
            val searchJobs = it.orEmpty()
            showSearchEmpty(searchJobs.isEmpty())
            searchAdapter.submitList(searchJobs)
        }
    }

    private fun observeJobData() {
        mRealVM.allPositioData.observe(viewLifecycleOwner) {
            val jobData = it.orEmpty()
            cyIyPnAdapter.submitList(jobData)
            submitRightJobList(jobData)

            val targetPosition = (mRealVM.lastSelectedPosition2.value ?: 1)
                .takeIf { position -> position in jobData.indices }
                ?: 0
            selectLeftJob(targetPosition, scrollLeft = false)
            binding.rcRight.post {
                if (!isViewDestroyed) {
                    scrollRightToJob(targetPosition, smooth = false)
                }
            }
        }
    }

    private fun showSearchEmpty(isEmpty: Boolean) {
        binding.tvEm.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rcSc.visibility = if (isEmpty) View.GONE else View.VISIBLE
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

    private fun submitRightJobList(data: List<CommonEnumItem>) {
        rightJobCategoryItems.clear()
        rightJobSections.clear()

        buildJobGroups(data).forEach { group ->
            appendJobGroup(group)
        }

        rightGroupDecoration.submitSections(rightJobSections.map { it.toStickyHeaderSection() })
        jobRightAdapter.submitList(rightJobCategoryItems.toList())
        binding.rcRight.post {
            if (!isViewDestroyed) {
                binding.rcRight.invalidateItemDecorations()
            }
        }
    }

    private fun buildJobGroups(data: List<CommonEnumItem>): List<JobGroup> {
        return data.mapIndexedNotNull { leftIndex, parent ->
            val secondCategories = parent.secondCategories.orEmpty()
                .takeIf { it.isNotEmpty() } ?: return@mapIndexedNotNull null
            JobGroup(
                leftIndex = leftIndex,
                secondCategories = secondCategories
            )
        }
    }

    private fun appendJobGroup(group: JobGroup) {
        group.secondCategories.forEach { secondCategory ->
            rightJobSections.add(
                JobSection(
                    leftIndex = group.leftIndex,
                    startPosition = rightJobCategoryItems.size,
                    title = secondCategory.secondCategoryName.orEmpty()
                        .ifBlank { secondCategory.name.orEmpty() }
                )
            )
            rightJobCategoryItems.add(secondCategory.copy(fieldCode = JOB_SECTION_HEADER_CODE))
            rightJobCategoryItems.addAll(secondCategory.positions.orEmpty())
        }
    }

    private fun selectLeftJob(position: Int, scrollLeft: Boolean = true) {
        if (isViewDestroyed) return
        val data = mRealVM.allPositioData.value ?: return
        if (position !in data.indices) return
        val oldPosition = mRealVM.lastSelectedPosition2.value
        val alreadySelected = data.getOrNull(position)?.isSelected == true
        if (oldPosition == position && alreadySelected) return
        data.forEachIndexed { index, item -> item.isSelected = index == position }
        mRealVM.lastSelectedPosition2.value = position
        cyIyPnAdapter.notifyDataSetChanged()
        if (scrollLeft) {
            leftLayoutManager?.scrollToPositionWithOffset(position, 0)
        }
    }

    private fun locateSelectedJob(item: CommonEnumItem) {
        if (isViewDestroyed) return
        val targetCode = item.positionCode ?: return
        val rightPosition = rightJobCategoryItems.indexOfFirst { rightItem ->
            !isJobSectionHeader(rightItem) && rightItem.positionCode == targetCode
        }
        if (rightPosition < 0) return
        val leftIndex = rightJobSections.lastOrNull { it.startPosition <= rightPosition }?.leftIndex ?: return
        selectLeftJob(leftIndex, scrollLeft = false)
        scrollLeftToJob(leftIndex)
        binding.rcRight.post {
            if (!isViewDestroyed) {
                scrollRightToJobCategory(rightPosition, suppressSync = true)
            }
        }
    }

    private fun scrollLeftToJob(position: Int) {
        if (isViewDestroyed) return
        leftLayoutManager?.scrollToPositionWithOffset(position, 0)
    }

    private fun scrollRightToJob(
        leftIndex: Int,
        smooth: Boolean = true,
        suppressSync: Boolean = false
    ) {
        if (isViewDestroyed || !isAdded) return
        if (suppressSync) {
            suppressRightScrollSync = true
        }
        val section = rightJobSections.firstOrNull { it.leftIndex == leftIndex } ?: return
        if (smooth) {
            val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                override fun getVerticalSnapPreference(): Int = SNAP_TO_START
            }
            smoothScroller.targetPosition = section.startPosition
            rightLayoutManager?.startSmoothScroll(smoothScroller)
        } else {
            rightLayoutManager?.scrollToPositionWithOffset(section.startPosition, 0)
        }
    }

    private fun scrollRightToJobCategory(rightPosition: Int, suppressSync: Boolean = false) {
        if (isViewDestroyed) return
        if (suppressSync) {
            suppressRightScrollSync = true
        }
        val offset = if (jobRightAdapter.isHeaderPosition(rightPosition)) {
            0
        } else {
            rightGroupDecoration.headerHeight
        }
        rightLayoutManager?.scrollToPositionWithOffset(rightPosition, offset)
    }

    private fun syncLeftByRightPosition(recyclerView: RecyclerView) {
        if (isViewDestroyed) return
        if (rightJobSections.isEmpty()) return
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION) return
        val section = rightJobSections.lastOrNull { it.startPosition <= firstVisible } ?: return
        selectLeftJob(section.leftIndex)
    }

    private fun isJobSectionHeader(item: CommonEnumItem): Boolean {
        return item.fieldCode == JOB_SECTION_HEADER_CODE
    }

    override fun onDestroyView() {
        isViewDestroyed = true
        runCatching {
            binding.rcRight.removeOnScrollListener(rightScrollListener)
        }
        super.onDestroyView()
    }

    private data class JobGroup(
        val leftIndex: Int,
        val secondCategories: List<CommonEnumItem>
    )

    private data class JobSection(
        val leftIndex: Int,
        val startPosition: Int,
        val title: String
    ) {
        fun toStickyHeaderSection(): StickyHeaderSection {
            return StickyHeaderSection(startPosition, title)
        }
    }
}
