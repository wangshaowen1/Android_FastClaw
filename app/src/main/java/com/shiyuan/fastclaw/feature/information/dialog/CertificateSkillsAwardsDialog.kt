package com.shiyuan.fastclaw.feature.information.dialog

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.adapter.SingleBindingAdapter
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldTree
import com.shiyuan.fastclaw.databinding.DialogCertificateSkillsAwardsBinding
import com.shiyuan.fastclaw.databinding.ItemCertificateSkillsAwardsBinding
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.feature.information.AwardActivity
import com.shiyuan.fastclaw.feature.information.CertificateActivity
import com.shiyuan.fastclaw.feature.information.SkillActivity
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class CertificateSkillsAwardsDialog : BaseBottomPushVMDialogFragment<DialogCertificateSkillsAwardsBinding, InformationViewModel>() {

    companion object {
        const val TAG = "CertificateSkillsAwardsDialog"
        private var onAddClick: ((String) -> Unit)? = null

        private var onCompleteClick: (() -> Unit)? = null
        private var onFieldTreeItemClick: ((AttributeFieldTree, AttributeArrayQueryData) -> Unit)? = null
        private var type: String = "certificates"

        fun show(
            manager: FragmentManager,
            attributeData: AttributeArrayQueryData? = null,
            type: String = "certificates",
            onCompleteClick: (() -> Unit)? = null,
            onAddClick: ((String) -> Unit)? = null,
            onFieldTreeItemClick: ((AttributeFieldTree, AttributeArrayQueryData) -> Unit)? = null,
        ): CertificateSkillsAwardsDialog {
            this.onAddClick = onAddClick
            this.onFieldTreeItemClick = onFieldTreeItemClick
            this.onCompleteClick = onCompleteClick
            this.type = type
            return CertificateSkillsAwardsDialog().apply {
                arguments = Bundle().apply {
                    attributeData?.let { putParcelable(ARG_ATTRIBUTE_DATA, it) }
                }
                show(manager)
            }
        }

        private const val ARG_ATTRIBUTE_DATA = "arg_attribute_data"
    }

    private var attributeData: AttributeArrayQueryData? = null

    private val fieldTreeAdapter by lazy {
        SingleBindingAdapter<ItemCertificateSkillsAwardsBinding, AttributeFieldTree>(
            R.layout.item_certificate_skills_awards,
            emptyList()
        ) { binding, position, item ->
            binding.tvName.text = item.displayValue ?: "暂无"
            binding.root.setOnClickListener {
                attributeData?.let { attr ->
                    when (type) {
                        "skills" -> {
                            SkillActivity.start(requireActivity(), true, attr, position)
                        }

                        "certificates" -> {
                            CertificateActivity.start(requireActivity(), true, attr, position)
                        }
                        else -> {
                            AwardActivity.start(requireActivity(), true, attr, position)
                        }
                    }
                }
            }
        }
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        attributeData = getAttributeData()
        initRecyclerView()
        initView()
        bindData()
    }

    private fun initRecyclerView() {
        binding.rvExperience.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        if (binding.rvExperience.itemDecorationCount == 0) {
            binding.rvExperience.addItemDecoration(SpaceDecoration(12.dp))
        }
    }

    private fun initView() {
        when (type) {
            "skills" -> {
                binding.tvTitle.text = "技能"
            }
            "certificates" -> {
                binding.tvTitle.text = "证书"
            }
            else -> {
                binding.tvTitle.text = "奖项"
            }
        }
        binding.ivClose.setOnSingleClickListener { dismiss() }
        binding.clAddExperience.setOnSingleClickListener {
            attributeData?.fieldTree?.let { treeList ->
                // 1. 取出第0个元素（如果存在）
                val original = treeList.getOrNull(0) ?: return@let

                // 2. 深拷贝 original（通过序列化方式最安全，此处展示手动递归拷贝）
                val copy = original.copy(
                    fieldItems = original.fieldItems?.map { item ->
                        item.copy(
                            fieldValue = null,      // 直接置 null
                            displayValue = null
                        )
                    }?.toMutableList()
                )
                // 3. 将副本插入到列表第0个位置
                treeList.add(0, copy)
            }
            when (type) {
                "skills" -> {
                    SkillActivity.start(requireActivity(), false, attributeData,0)
                }
                "certificates" -> {
                    CertificateActivity.start(requireActivity(), false, attributeData,0)
                }

                else -> {
                    AwardActivity.start(requireActivity(), false, attributeData,0)
                }
            }
        }
        binding.clWcExperience.setOnSingleClickListener {
            dismiss()
            onCompleteClick?.invoke()
        }
    }

    private fun bindData() {
        val fieldTree = attributeData?.fieldTree ?: emptyList()
        binding.rvExperience.adapter = fieldTreeAdapter
        @SuppressLint("NotifyDataSetChanged")
        fieldTreeAdapter.submitList(fieldTree)
    }

    @Suppress("DEPRECATION")
    private fun getAttributeData(): AttributeArrayQueryData? {
        val bundle = arguments ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(ARG_ATTRIBUTE_DATA, AttributeArrayQueryData::class.java)
        } else {
            bundle.getParcelable(ARG_ATTRIBUTE_DATA)
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
    }
}
