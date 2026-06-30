package com.shiyuan.fastclaw.feature.mine

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.viewmodel.EmptyViewModel
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.FieldItems
import com.shiyuan.fastclaw.core.network.apis.response.formatValueToString
import com.shiyuan.fastclaw.databinding.FragmentResumeInfoBinding
import com.shiyuan.fastclaw.databinding.ItemJobPreference2Binding
import com.shiyuan.fastclaw.databinding.ItemJobPreference2SonBinding
import com.shiyuan.fastclaw.feature.mine.viewModel.UnderstandsViewModel
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration

class ResumeInfoFragment : BaseViewModelFragment<FragmentResumeInfoBinding, EmptyViewModel>() {

    companion object {
        fun newInstance(): ResumeInfoFragment = ResumeInfoFragment()
    }

    private val parentVm by lazy {
        getViewModelOfActivity<UnderstandsViewModel>()
    }
    private val expandState = mutableMapOf<Int, Boolean>()
    private val resumeAdapter by lazy {
        SingleBindingAdapter<ItemJobPreference2Binding, FieldItems>(
            R.layout.item_job_preference2,
            emptyList(),
            onBind = { itemBinding, position, item ->
                itemBinding.tvTile.text = item.fieldName ?: ""
                if (item.fieldItems.isEmpty()) {
                    itemBinding.ll.visibility = View.VISIBLE
                    itemBinding.rcConnect.visibility = View.GONE
                    itemBinding.tvLlTile.text = item.fieldName ?: ""
                    itemBinding.tvLlConnect.text = item.fieldValue?.toString() ?: ""
                    itemBinding.llExpand.visibility = View.GONE
                } else {
                    itemBinding.ll.visibility = View.GONE
                    itemBinding.rcConnect.visibility = View.VISIBLE
                    bindChildItems(itemBinding, position, item)
                }
            }
        )
    }

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        initObservers()
    }

    private fun initView() {
        binding.rvResumeInfo.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
            addItemDecoration(SpaceDecoration(16.dp, SpaceDecoration.Direction.MIDDLE))
            adapter = resumeAdapter
        }
    }

    private fun initObservers() {
        parentVm?.resumeInfo?.observe(this) { data ->
            updateResumeInfo(data.orEmpty())
        }
        parentVm?.titleInfo?.observe(this) { data ->
            updateProfile(data.orEmpty())
        }
    }

    private fun updateResumeInfo(data: List<FieldItems>) {
        val showEmpty = data.isEmpty()
        binding.viewUnderstands.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.rvResumeInfo.visibility = if (showEmpty) View.GONE else View.VISIBLE
        resumeAdapter.submitList(convertToTwoLevels(mergeFieldItems(data)))
    }

    private fun updateProfile(data: List<FieldItems>) {
        binding.tvName.text = findFieldValue(data, "name").ifBlank { "暂无" }
        binding.tvTip.text = findFieldValue(data, "jobStatus").ifBlank { "求职状态" }
        updateContact(binding.layoutProfilePhone, binding.tvProfilePhone, findFieldValue(data, "phone"))
        updateContact(binding.layoutProfileEmail, binding.tvProfileEmail, findFieldValue(data, "email"))
        updateContact(binding.layoutProfileWechat, binding.tvProfileWechat, findFieldValue(data, "wechat"))
    }

    private fun updateContact(layout: View, textView: android.widget.TextView, value: String) {
        layout.visibility = if (value.isBlank()) View.GONE else View.VISIBLE
        textView.text = value
    }

    private fun findFieldValue(data: List<FieldItems>, fieldCode: String): String {
        return data.firstOrNull { it.fieldCode == fieldCode }?.fieldValue?.toString().orEmpty()
    }

    private fun bindChildItems(
        itemBinding: ItemJobPreference2Binding,
        position: Int,
        item: FieldItems
    ) {
        val totalSize = item.fieldItems.size
        val isExpanded = expandState[position] == true
        val displayList = if (isExpanded) item.fieldItems else item.fieldItems.take(4)
        val subAdapter = SingleBindingAdapter<ItemJobPreference2SonBinding, FieldItems>(
            R.layout.item_job_preference2_son,
            displayList,
            onBind = { sonBinding, _, sonItem ->
                val hasExtra = item.fieldItems.any { it.extra != null }
                if (hasExtra && sonItem.extra.isNullOrEmpty()) {
                    sonBinding.tvParentTile.visibility = View.VISIBLE
                    sonBinding.llSon.visibility = View.GONE
                    sonBinding.tvParentTile.text = sonItem.fieldValue?.toString() ?: ""
                } else {
                    sonBinding.tvParentTile.visibility = View.GONE
                    sonBinding.llSon.visibility = View.VISIBLE
                    sonBinding.tvTile.text = sonItem.fieldName ?: ""
                    sonBinding.tvConnect.text = formatValueToString(sonItem.fieldValue)
                }
            }
        )
        itemBinding.rcConnect.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            isNestedScrollingEnabled = false
            adapter = subAdapter
        }
        if (totalSize > 4) {
            itemBinding.llExpand.visibility = View.VISIBLE
            itemBinding.tvExpand.text = if (isExpanded) "收起全部" else "展开全部"
            itemBinding.ivArrow.rotation = if (isExpanded) 180f else 0f
            itemBinding.llExpand.setOnSingleClickListener {
                expandState[position] = !isExpanded
                resumeAdapter.notifyItemChanged(position)
            }
        } else {
            itemBinding.llExpand.visibility = View.GONE
        }
    }

    private fun convertToTwoLevels(items: List<FieldItems>): List<FieldItems> {
        return items.map { convertTopLevelItem(it) }
    }

    private fun convertTopLevelItem(item: FieldItems): FieldItems {
        val hasNestedChildren = item.fieldItems.any { it.fieldItems.isNotEmpty() }
        if (!hasNestedChildren) return item
        val newFieldItems = mutableListOf<FieldItems>()
        for (groupItem in item.fieldItems) {
            val groupNodeWithoutChildren = FieldItems(
                fieldCode = groupItem.fieldCode,
                fieldName = groupItem.fieldName,
                fieldType = groupItem.fieldType,
                fieldValue = groupItem.fieldValue,
                fieldItems = emptyList(),
                extra = groupItem.extra
            )
            newFieldItems.add(groupNodeWithoutChildren)
            val parentTag = groupItem.fieldValue ?: groupItem.fieldName ?: groupItem.fieldCode ?: ""
            for (child in groupItem.fieldItems) {
                val mergedExtra = (child.extra ?: emptyMap()).toMutableMap()
                mergedExtra["parent"] = parentTag
                val liftedChild = FieldItems(
                    fieldCode = child.fieldCode,
                    fieldName = child.fieldName,
                    fieldType = child.fieldType,
                    fieldValue = child.fieldValue,
                    fieldItems = child.fieldItems,
                    extra = mergedExtra
                )
                newFieldItems.add(liftedChild)
            }
        }
        return FieldItems(
            fieldCode = item.fieldCode,
            fieldName = item.fieldName,
            fieldType = item.fieldType,
            fieldValue = item.fieldValue,
            fieldItems = newFieldItems,
            extra = item.extra
        )
    }

    private fun mergeFieldItems(items: List<FieldItems>): List<FieldItems> {
        if (items.isEmpty()) return emptyList()
        val processed = items.map { item ->
            FieldItems(
                fieldCode = item.fieldCode,
                fieldName = item.fieldName,
                fieldType = item.fieldType,
                fieldValue = item.fieldValue,
                fieldItems = mergeFieldItems(item.fieldItems),
                extra = item.extra
            )
        }
        val grouped = processed.filter { it.fieldCode != null }.groupBy { it.fieldCode!! }
        val result = mutableListOf<FieldItems>()
        for ((code, group) in grouped) {
            val allLeaf = group.all { it.fieldItems.isEmpty() }
            if (allLeaf && group.size > 1) {
                val mergedValue = group.mapNotNull { it.fieldValue?.toString() }
                    .filter { it.isNotBlank() }
                    .joinToString(",")
                    .takeIf { it.isNotEmpty() }
                val first = group.first()
                result.add(
                    FieldItems(
                        fieldCode = code,
                        fieldName = first.fieldName,
                        fieldType = first.fieldType,
                        fieldValue = mergedValue,
                        fieldItems = emptyList(),
                        extra = first.extra
                    )
                )
            } else {
                result.addAll(group)
            }
        }
        result.addAll(processed.filter { it.fieldCode == null })
        return result
    }
}
