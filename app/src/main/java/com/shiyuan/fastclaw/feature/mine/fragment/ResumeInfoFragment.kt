package com.shiyuan.fastclaw.feature.mine.fragment

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.BaseViewModelFragment
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.databinding.FragmentResumeInfoBinding
import com.shiyuan.fastclaw.databinding.ItemResumeInfoBinding
import com.shiyuan.fastclaw.feature.mine.viewModel.UnderstandsViewModel

class ResumeInfoFragment : BaseViewModelFragment<FragmentResumeInfoBinding, UnderstandsViewModel>() {

    companion object {
        fun newInstance(): ResumeInfoFragment = ResumeInfoFragment()
    }

    private val resumeInfoAdapter by lazy {
        SingleBindingAdapter<ItemResumeInfoBinding, AttributeArrayQueryData>(
            R.layout.item_resume_info,
            emptyList(),
            onBind = { itemBinding, _, item ->
                bindResumeInfoItem(itemBinding, item)
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
            adapter = resumeInfoAdapter
        }
        updateResumeInfo(mockResumeInfoData())
    }

    private fun initObservers() {

    }

    fun submitResumeInfo(data: List<AttributeArrayQueryData>?) {
        updateResumeInfo(data.orEmpty())
    }

    private fun updateResumeInfo(data: List<AttributeArrayQueryData>) {
        val showEmpty = data.isEmpty()
        binding.viewUnderstands.root.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.rvResumeInfo.visibility = if (showEmpty) View.GONE else View.VISIBLE
        resumeInfoAdapter.submitList(data)
    }

    private fun bindResumeInfoItem(
        itemBinding: ItemResumeInfoBinding,
        item: AttributeArrayQueryData
    ) {
        itemBinding.tvTitle.text = item.displayTitle()
        itemBinding.layoutMissing.visibility = if (item.fieldTree == null) View.VISIBLE else View.GONE
        itemBinding.ivEdit.setImageResource(item.editIconRes())
    }

    private fun AttributeArrayQueryData.displayTitle(): String {
        return attributeGroupName?.takeIf { it.isNotBlank() }
            ?: fieldTree?.firstOrNull()?.fieldName?.takeIf { it.isNotBlank() }
            ?: attributeGroupCode.orEmpty()
    }

    private fun mockResumeInfoData(): List<AttributeArrayQueryData> {
        return listOf(
            mockResumeInfoItem("selfEvaluation", "自我评价"),
            mockResumeInfoItem("workExperience", "工作经历"),
            mockResumeInfoItem("projectExperience", "项目经历"),
            mockResumeInfoItem("educationExperience", "教育经历"),
            mockResumeInfoItem("skill", "技能"),
            mockResumeInfoItem("award", "奖项"),
            mockResumeInfoItem("certificate", "证书")
        )
    }

    private fun mockResumeInfoItem(code: String, title: String): AttributeArrayQueryData {
        return AttributeArrayQueryData(
            attributeGroupCode = code,
            attributeGroupName = title,
            fieldTree = null
        )
    }

    private fun AttributeArrayQueryData.editIconRes(): Int {
        return if (attributeGroupCode == "selfEvaluation") {
            R.drawable.ic_online_resume_profile_edit
        } else {
            R.drawable.ic_online_resume_add
        }
    }

}
