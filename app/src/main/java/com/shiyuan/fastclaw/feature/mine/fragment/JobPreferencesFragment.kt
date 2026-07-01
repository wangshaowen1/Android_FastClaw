package com.shiyuan.fastclaw.feature.mine.fragment

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.EmptyViewModel
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.common.ext.gone
import com.shiyuan.fastclaw.common.ext.visible
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldTree
import com.shiyuan.fastclaw.databinding.FragmentJobPreferencesBinding
import com.shiyuan.fastclaw.databinding.ItemJobInfoBinding
import com.shiyuan.fastclaw.databinding.ItemJobPreference1Binding
import com.shiyuan.fastclaw.databinding.ItemJobPreference1SonBinding
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration

class JobPreferencesFragment :
    BaseViewModelFragment<FragmentJobPreferencesBinding, EmptyViewModel>() {

    companion object {
        fun newInstance(): JobPreferencesFragment = JobPreferencesFragment()
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
        itemBinding.ivEdit.visibility = View.INVISIBLE
        val directText = item.directDisplayText()
        val showDirectText = directText.isNotBlank()
        itemBinding.tvName.text = directText
        itemBinding.tvName.visibility = if (showDirectText) View.VISIBLE else View.GONE
        itemBinding.layoutMissing.visibility =
            if (item.fieldTree.isNullOrEmpty() && !showDirectText) View.VISIBLE else View.INVISIBLE

        if (showDirectText) {
            itemBinding.rcFieldTree.visibility = View.GONE
            itemBinding.rcFieldTree.adapter = null
        } else {
            bindFieldTreeList(itemBinding, item.fieldTree)
        }
    }

    private fun AttributeArrayQueryData.displayTitle(): String {
        return attributeGroupName?.takeIf { it.isNotBlank() }
            ?: fieldTree?.firstOrNull()?.fieldName?.takeIf { it.isNotBlank() }
            ?: attributeGroupCode.orEmpty()
    }

    private fun AttributeArrayQueryData.directDisplayText(): String {
        val onlyTree = fieldTree?.takeIf { it.size == 1 }?.firstOrNull() ?: return ""
        if (!onlyTree.fieldItems.isNullOrEmpty()) return ""
        return onlyTree.displayText()
    }

    private fun bindFieldTreeList(
        itemBinding: ItemJobInfoBinding,
        fieldTree: List<AttributeFieldTree>?
    ) {
        if (fieldTree.isNullOrEmpty()) {
            itemBinding.rcFieldTree.visibility = View.GONE
            itemBinding.rcFieldTree.adapter = null
            return
        }
        itemBinding.rcFieldTree.visibility = View.VISIBLE
        itemBinding.rcFieldTree.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        itemBinding.rcFieldTree.isNestedScrollingEnabled = false
        if (itemBinding.rcFieldTree.itemDecorationCount == 0) {
            itemBinding.rcFieldTree.addItemDecoration(SpaceDecoration(12.dp))
        }
        itemBinding.rcFieldTree.adapter = createFieldTreeAdapter(fieldTree)
    }

    private fun createFieldTreeAdapter(
        fieldTree: List<AttributeFieldTree>
    ): SingleBindingAdapter<ItemJobPreference1Binding, AttributeFieldTree> {
        return SingleBindingAdapter(
            R.layout.item_job_preference1,
            fieldTree,
            onBind = { itemBinding, _, item ->
                bindJobPreferenceFieldTreeItem(itemBinding, item)
            }
        )
    }

    private fun bindJobPreferenceFieldTreeItem(
        itemBinding: ItemJobPreference1Binding,
        item: AttributeFieldTree
    ) {
        itemBinding.tvTile.text = item.fieldName?.takeIf { it.isNotBlank() } ?: item.fieldCode.orEmpty()
        val fieldItems = item.fieldItems.orEmpty()
        when {
            fieldItems.size > 1 -> bindTagList(itemBinding, fieldItems)
            fieldItems.size == 1 -> {
                itemBinding.rcConnect.visibility = View.GONE
                itemBinding.tvConnect.visibility = View.VISIBLE
                itemBinding.tvConnect.text = fieldItems.first().displayText()
            }
            else -> {
                itemBinding.rcConnect.visibility = View.GONE
                itemBinding.tvConnect.visibility = View.VISIBLE
                itemBinding.tvConnect.text = item.displayText()
            }
        }
    }

    private fun bindTagList(
        itemBinding: ItemJobPreference1Binding,
        fieldItems: List<AttributeFieldItem>
    ) {
        itemBinding.tvConnect.visibility = View.GONE
        itemBinding.rcConnect.visibility = View.VISIBLE
        val subAdapter = SingleBindingAdapter<ItemJobPreference1SonBinding, AttributeFieldItem>(
            R.layout.item_job_preference1_son,
            fieldItems,
            onBind = { tagBinding, _, sub ->
                tagBinding.tvTag.text = sub.displayText()
                val maxTagWidth = itemBinding.rcConnect.measuredWidth
                    .takeIf { it > 0 }
                    ?: (resources.displayMetrics.widthPixels - 64.dp)
                tagBinding.tvTag.maxWidth = maxTagWidth.coerceAtLeast(0)
                (tagBinding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                    it.marginEnd = 10.dp
                    it.bottomMargin = 10.dp
                    tagBinding.root.layoutParams = it
                }
            }
        )
        itemBinding.rcConnect.apply {
            layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.FLEX_START
                alignItems = AlignItems.FLEX_START
            }
            isNestedScrollingEnabled = false
            adapter = subAdapter
            post { requestLayout() }
        }
    }

    private fun mockJobPreferenceData(): List<AttributeArrayQueryData> {
        return listOf(
            mockJobPreferenceItem(
                code = "jobStatus",
                title = "求职状态",
                fieldTree = mutableListOf(
                    AttributeFieldTree(
                        fieldCode = "jobStatus",
                        displayValue = "在职-正在找"
                    )
                )
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
        fieldTree: MutableList<AttributeFieldTree>
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
            ?: findFirstDisplayText(fieldItems).orEmpty()
    }

    private fun AttributeFieldItem.displayText(): String {
        return displayValue?.takeIf { it.isNotBlank() }
            ?: fieldValue?.toString()?.takeIf { it.isNotBlank() }
            ?: fieldName.orEmpty()
    }

    private fun findFirstDisplayText(items: List<AttributeFieldItem>?): String? {
        items.orEmpty().forEach { item ->
            item.displayText().takeIf { it.isNotBlank() }?.let { return it }
            findFirstDisplayText(item.fieldItems)?.let { return it }
        }
        return null
    }
}
