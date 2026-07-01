package com.shiyuan.fastclaw.feature.mine.fragment

import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.EmptyViewModel
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldTree
import com.shiyuan.fastclaw.databinding.FragmentJobPreferencesBinding
import com.shiyuan.fastclaw.databinding.ItemJobInfoBinding
import com.shiyuan.fastclaw.databinding.ItemJobInfoReBinding
import com.shiyuan.fastclaw.feature.mine.fragment.ResumeInfoFragment.Companion.INCOMPLETE
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration


class JobPreferencesFragment :
    BaseViewModelFragment<FragmentJobPreferencesBinding, EmptyViewModel>() {

    companion object {
        fun newInstance(): JobPreferencesFragment = JobPreferencesFragment()

        private const val CODE_JOB_STATUS = "jobStatus"
    }

    private val preferenceAdapter by lazy {
        SingleBindingAdapter<ItemJobInfoBinding, AttributeArrayQueryData>(
            R.layout.item_job_info,
            emptyList(),
            onBind = { itemBinding, _, item ->
                bindJobInfoItem(itemBinding, item)
            }
        )
    }


    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObservers()
    }

    private fun initView() {
        binding.rvJobPreferences.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
            adapter = preferenceAdapter
        }
        updateJobPreferences(mockJobPreferenceData())
    }

    private fun initObservers() {

    }

    fun submitJobPreferences(data: List<AttributeArrayQueryData>?) {
        updateJobPreferences(data.orEmpty())
    }

    private fun updateJobPreferences(data: List<AttributeArrayQueryData>) {
        val showEmpty = data.isEmpty()
        binding.viewUnderstands.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.rvJobPreferences.visibility = if (showEmpty) View.GONE else View.VISIBLE
        preferenceAdapter.submitList(data)
    }

    private fun bindJobInfoItem(
        itemBinding: ItemJobInfoBinding,
        item: AttributeArrayQueryData
    ) {
        itemBinding.tvTitle.text = item.displayTitle()
        if (item.attributeGroupCode == CODE_JOB_STATUS) {
            itemBinding.ll.setBackgroundResource(R.drawable.bg_f3f9ff_ffffff_r12)
            val paddingTopPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, itemBinding.ll.getResources().getDisplayMetrics()).toInt()
            itemBinding.ll.setPadding(itemBinding.ll.getPaddingLeft(), paddingTopPx, itemBinding.ll.getPaddingRight(), itemBinding.ll.getPaddingBottom());
            itemBinding.ivEdit.visibility = View.VISIBLE
            itemBinding.viewIndicator.visibility = View.GONE
            itemBinding.tvName.visibility = if (item.fieldTree.isNullOrEmpty()) View.GONE else View.VISIBLE
            itemBinding.tvName.text = item.fieldTree?.firstOrNull()?.displayValue?.toString().orEmpty()
            itemBinding.layoutMissing.visibility = if (item.fieldTree.isNullOrEmpty()) View.VISIBLE else View.INVISIBLE
            itemBinding.rcFieldTree.visibility = View.GONE
            itemBinding.rcFieldTree.adapter = null
        } else {
            itemBinding.ll.setBackgroundResource(R.color.white)
            itemBinding.ivEdit.visibility = View.VISIBLE
            itemBinding.ivEdit.visibility = View.INVISIBLE
            itemBinding.tvName.visibility = View.GONE
            itemBinding.layoutMissing.visibility = View.INVISIBLE
            bindFieldTreeList(itemBinding, item.fieldTree)
        }
    }

    private fun AttributeArrayQueryData.displayTitle(): String {
        return attributeGroupName?.takeIf { it.isNotBlank() }
            ?: fieldTree?.firstOrNull()?.fieldName?.takeIf { it.isNotBlank() }
            ?: attributeGroupCode.orEmpty()
    }

    private fun bindFieldTreeList(
        itemBinding: ItemJobInfoBinding,
        fieldTree: List<AttributeFieldTree>?
    ) {
        itemBinding.rcFieldTree.visibility = View.VISIBLE
        itemBinding.rcFieldTree.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        itemBinding.rcFieldTree.isNestedScrollingEnabled = false
        if (itemBinding.rcFieldTree.itemDecorationCount == 0) {
            itemBinding.rcFieldTree.addItemDecoration(SpaceDecoration(12.dp))
        }
        itemBinding.rcFieldTree.adapter = createFieldTreeAdapter(fieldTree.orEmpty())
    }

    private fun createFieldTreeAdapter(
        fieldTree: List<AttributeFieldTree>
    ): SingleBindingAdapter<ItemJobInfoReBinding, AttributeFieldTree> {
        return SingleBindingAdapter(
            R.layout.item_job_info_re,
            fieldTree,
            onBind = { itemBinding, _, item ->
                bindJobInfoReItem(itemBinding, item)
            }
        )
    }

    private fun bindJobInfoReItem(
        itemBinding: ItemJobInfoReBinding,
        item: AttributeFieldTree
    ) {
        itemBinding.layoutMissing.visibility = if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE
        itemBinding.tvVale.visibility = if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE
        val valueText = item.displayText()
        itemBinding.tvName.text = item.fieldName?.takeIf { it.isNotBlank() } ?: item.fieldCode.orEmpty()
        itemBinding.tvVale.text = valueText

    }

    private fun mockJobPreferenceData(): List<AttributeArrayQueryData> {
        return listOf(
            mockJobPreferenceItem(
                code = CODE_JOB_STATUS,
                title = "求职状态"
            ),
            mockJobPreferenceItem(
                code = "jobExpectation",
                title = "求职期望",
                fieldTree = mutableListOf(
                    mockFieldTree("expectedCity", "期望城市", "深圳"),
                    mockFieldTree("expectedPosition", "期望职位", "AI训练师"),
                    mockFieldTree("expectedSalary", "期望薪资", "20k-30k")
                )
            ),
            mockJobPreferenceItem(
                code = "coreIntention",
                title = "求职核心意向",
                fieldTree = mutableListOf(
                    mockTagTree("coreIntention", "核心意向", listOf("管理方向", "全职", "不符合预期薪资"))
                )
            ),
            mockJobPreferenceItem(
                code = "workCondition",
                title = "工作条件",
                fieldTree = mutableListOf(
                    mockTagTree("workCondition", "工作条件", listOf("坐班", "是", "可接受996", "可接受出差"))
                )
            ),
            mockJobPreferenceItem(
                code = "environmentWelfare",
                title = "环境与福利",
                fieldTree = mutableListOf(
                    mockTagTree("environmentWelfare", "偏好福利", listOf("优先大公司", "高底薪", "扁平化", "五险一金"))
                )
            )
        )
    }

    private fun mockJobPreferenceItem(
        code: String,
        title: String,
        fieldTree: MutableList<AttributeFieldTree>? = null
    ): AttributeArrayQueryData {
        return AttributeArrayQueryData(
            attributeGroupCode = code,
            attributeGroupName = title,
            fieldTree = fieldTree
        )
    }

    private fun mockFieldTree(
        fieldCode: String,
        fieldName: String,
        displayValue: String
    ): AttributeFieldTree {
        return AttributeFieldTree(
            fieldCode = fieldCode,
            fieldName = fieldName,
            displayValue = displayValue
        )
    }

    private fun mockTagTree(
        fieldCode: String,
        fieldName: String,
        values: List<String>
    ): AttributeFieldTree {
        return AttributeFieldTree(
            fieldCode = fieldCode,
            fieldName = fieldName,
            fieldItems = values.map { value ->
                AttributeFieldItem(displayValue = value)
            }
        )
    }

    private fun AttributeFieldTree.displayText(): String {
        return displayValue?.takeIf { it.isNotBlank() }
            ?: fieldValue?.toString()?.takeIf { it.isNotBlank() }
            ?: fieldItems.orEmpty()
                .mapNotNull { it.displayText().takeIf { text -> text.isNotBlank() } }
                .joinToString("、")
    }

    private fun AttributeFieldItem.displayText(): String {
        return displayValue?.takeIf { it.isNotBlank() }
            ?: fieldValue?.toString()?.takeIf { it.isNotBlank() }
            ?: fieldItems.orEmpty()
                .mapNotNull { it.displayText().takeIf { text -> text.isNotBlank() } }
                .joinToString("、")
                .takeIf { it.isNotBlank() }
            ?: fieldName.orEmpty()
    }
}
