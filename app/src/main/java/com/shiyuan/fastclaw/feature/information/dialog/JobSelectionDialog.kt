package com.shiyuan.fastclaw.feature.information.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.toJsonString
import com.shiyuan.fastclaw.common.utils.KeyboardUtils.hideKeyboard
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.DialogJobSelectionBinding
import com.shiyuan.fastclaw.feature.information.adapter.IyPnAdapter
import com.shiyuan.fastclaw.feature.information.adapter.IyPnJobSonAdapter
import com.shiyuan.fastclaw.feature.information.adapter.SearchAdapter
import com.shiyuan.fastclaw.feature.information.adapter.SelectedIyPnAdapter
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

/**
 * 职位选择底部弹窗：复用职位选择页的单选、多选、搜索与完成回传逻辑。
 */
class JobSelectionDialog :
    BaseBottomPushVMDialogFragment<DialogJobSelectionBinding, InformationViewModel>() {

    companion object {
        const val TAG = "JobSelectionDialog"

        private const val ARG_IS_MULTIPLE = "arg_is_multiple"
        private const val ARG_SESSION_ID = "arg_session_id"
        private const val ARG_CONVERSATION_ID = "arg_conversation_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MULTIPLE_NUM = "arg_multiple_num"
        private const val ARG_TYPE = "arg_type"

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

    private val cyIyPnAdapter: IyPnAdapter by lazy {
        IyPnAdapter({ item, position ->
            mRealVM.allPositioData.value?.forEach { it.isSelected = false }
            item.isSelected = true
            cyIyPnAdapter.notifyDataSetChanged()
            cyIyJobPnSonAdapter.submitList(
                mRealVM.allPositioData.value?.get(position)?.secondCategories
            )
            mRealVM.lastSelectedPosition2.value = position
        }, 1)
    }

    private val cyIyJobPnSonAdapter: IyPnJobSonAdapter by lazy {
        IyPnJobSonAdapter { item, _ ->
            if (isMultiple) {
                val selectedList = mRealVM.selectPositionData.value?.toMutableList() ?: mutableListOf()
                if (item.isSelected) {
                    item.isSelected = false
                    mRealVM.selectPositionData.value =
                        selectedList.filterNot { it.positionCode == item.positionCode }
                } else {
                    if (multipleNum == 1) {
                        selectOnlyPosition(item)
                    } else {
                        if (selectedList.size >= multipleNum) {
                            "最多选择${multipleNum}个".showToast()
                            return@IyPnJobSonAdapter
                        }
                        item.isSelected = true
                        mRealVM.selectPositionData.value =
                            (selectedList + item).distinctBy { it.positionCode }
                    }
                }
            } else {
                item.fieldCode = "position"
                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item))
                dismissAllowingStateLoss()
            }
            cyIyJobPnSonAdapter.notifyDataSetChanged()
        }
    }

    private val selectCyIyPnAdapter: SelectedIyPnAdapter by lazy {
        SelectedIyPnAdapter({ item ->
            val newList = mRealVM.selectPositionData.value
                ?.filterNot { it.positionCode == item.positionCode }
            mRealVM.selectPositionData.value = newList

            mRealVM.allPositioData.value?.forEach { parent ->
                parent.secondCategories?.forEach { secondCategory ->
                    secondCategory.positions
                        ?.firstOrNull { child -> child.positionCode == item.positionCode }
                        ?.isSelected = false
                }
            }
            cyIyJobPnSonAdapter.notifyDataSetChanged()
        }, 1)
    }

    private val searchAdapter: SearchAdapter by lazy {
        SearchAdapter({ item, _ ->
            if (isMultiple) {
                if (multipleNum == 1) {
                    selectOnlyPosition(item)
                } else {
                    val selectedList = mRealVM.selectPositionData.value?.toMutableList() ?: mutableListOf()
                    if (selectedList.size >= multipleNum) {
                        "最多选择${multipleNum}个".showToast()
                        return@SearchAdapter
                    }
                    item.isSelected = true
                    mRealVM.selectPositionData.value =
                        (selectedList + item).distinctBy { it.positionCode }

                    mRealVM.allPositioData.value?.forEach { parent ->
                        parent.secondCategories?.forEach { secondCategory ->
                            secondCategory.positions
                                ?.firstOrNull { child -> child.positionCode == item.positionCode }
                                ?.isSelected = true
                        }
                    }
                }
                cyIyJobPnSonAdapter.notifyDataSetChanged()
                binding.rlSc.visibility = View.GONE
                hideKeyboard(binding.et)
            } else {
                item.fieldCode = "position"
                FlowEventBus.postSticky(Events.CommonEnumItemEvent(item))
                dismissAllowingStateLoss()
            }
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
        initView()
        observeViewModel()
        mRealVM.queryDictPositionQuery(sessionId, conversationId)
    }

    private fun selectOnlyPosition(item: CommonEnumItem) {
        val selectedCode = item.positionCode
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
                mRealVM.lastSelectedPosition2.value = parentIndex
                cyIyJobPnSonAdapter.submitList(parent.secondCategories ?: emptyList())
            }
        }
        item.isSelected = true
        mRealVM.selectPositionData.value = listOf(item)
        cyIyPnAdapter.notifyDataSetChanged()
    }

    private fun initView() {
        if (isMultiple) {
            binding.tvTitle.text = getString(R.string.choose_desired_position)
            binding.llMultipleTop.visibility = View.GONE
            binding.llMultipleBot.visibility = View.VISIBLE
            binding.tvAllCount.text = "/$multipleNum"
            binding.rcSelected.apply {
                layoutManager = LinearLayoutManager(
                    requireContext(),
                    RecyclerView.HORIZONTAL,
                    false
                )
                isNestedScrollingEnabled = false
                adapter = selectCyIyPnAdapter
            }
        } else {
            binding.tvTitle.text = getString(R.string.select_job_title)
            binding.llMultipleTop.visibility = View.GONE
            binding.llMultipleBot.visibility = View.GONE
        }

        if (title.isNotEmpty()) {
            binding.tvTitle.text = title
        }

        binding.rcLeft.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                RecyclerView.VERTICAL,
                false
            )
            adapter = cyIyPnAdapter
        }

        binding.rcRight.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                RecyclerView.VERTICAL,
                false
            )
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            itemAnimator = null
            adapter = cyIyJobPnSonAdapter
        }

        binding.rcSc.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                RecyclerView.VERTICAL,
                false
            )
            adapter = searchAdapter
        }

        binding.et.doAfterTextChanged { editable ->
            val keyword = editable?.toString().orEmpty()
            val showIcon = keyword.isNotEmpty()
            if (showIcon) {
                mRealVM.queryDictPositionQuery(keyword, sessionId, conversationId)
                binding.ivClear.visibility = View.VISIBLE
                binding.rlSc.visibility = View.VISIBLE
            } else {
                binding.rlSc.visibility = View.GONE
                binding.ivClear.visibility = View.GONE
            }
        }

        binding.ivClear.setOnSingleClickListener {
            binding.rlSc.visibility = View.GONE
            binding.et.text.clear()
        }
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
        mRealVM.selectPositionData.observe(viewLifecycleOwner) {
            binding.tvWordCount.text = it.size.toString()
            selectCyIyPnAdapter.submitList(it)
            cyIyJobPnSonAdapter.notifyDataSetChanged()
        }

        mRealVM.searchPositionData.observe(viewLifecycleOwner) {
            if (it.isNullOrEmpty()) {
                binding.tvEm.visibility = View.VISIBLE
                binding.rcSc.visibility = View.GONE
            } else {
                binding.tvEm.visibility = View.GONE
                binding.rcSc.visibility = View.VISIBLE
            }
            searchAdapter.submitList(it)
        }

        mRealVM.allPositioData.observe(viewLifecycleOwner) {
            cyIyPnAdapter.submitList(it)

            mRealVM.allPositioData.value
                ?.getOrNull(mRealVM.lastSelectedPosition2.value ?: 1)
                ?.also { node ->
                    node.isSelected = true
                    cyIyJobPnSonAdapter.submitList(node.secondCategories ?: emptyList())
                } ?: cyIyJobPnSonAdapter.submitList(emptyList())
        }
    }
}
