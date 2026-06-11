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
import com.shiyuan.fastclaw.databinding.DialogEducationExperienceBinding
import com.shiyuan.fastclaw.databinding.ItemEducationExperienceBinding
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.isNull
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItemFinder
import com.shiyuan.fastclaw.feature.information.EducationalActivity
import com.shiyuan.fastclaw.feature.information.ProjectActivity
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class EducationExperienceDialog : BaseBottomPushVMDialogFragment<DialogEducationExperienceBinding, InformationViewModel>() {

    companion object {
        const val TAG = "EducationExperienceDialog"
        private var onAddClick: (() -> Unit)? = null
        private var onFieldTreeItemClick: ((AttributeFieldTree, AttributeArrayQueryData) -> Unit)? = null

        private var onCompleteClick: (() -> Unit)? = null


        fun show(
            manager: FragmentManager,
            attributeData: AttributeArrayQueryData? = null,
            onAddClick: (() -> Unit)? = null,
            onFieldTreeItemClick: ((AttributeFieldTree, AttributeArrayQueryData) -> Unit)? = null,
            onCompleteClick: (() -> Unit)? = null,
        ): EducationExperienceDialog {
            this.onAddClick = onAddClick
            this.onFieldTreeItemClick = onFieldTreeItemClick
            this.onCompleteClick = onCompleteClick
            return EducationExperienceDialog().apply {
                arguments = Bundle().apply {
                    attributeData?.let { putParcelable(ARG_ATTRIBUTE_DATA, it) }
                }
                show(manager)
            }
        }

        private const val ARG_ATTRIBUTE_DATA = "arg_attribute_data"
        private const val INCOMPLETE = "INCOMPLETE"
        private const val START_DATE = "startDate"
        private const val END_DATE = "endDate"
        private const val MAJOR = "major"
    }

    private var attributeData: AttributeArrayQueryData? = null

    private val fieldTreeAdapter by lazy {
        SingleBindingAdapter<ItemEducationExperienceBinding, AttributeFieldTree>(
            R.layout.item_education_experience,
            emptyList()
        ) { binding, position, item ->
            binding.tvCompany.text = item.displayValue ?: "暂无"
            binding.layoutMissing.visibility = if (item.statusCode == INCOMPLETE) View.VISIBLE else View.GONE
            val startDate = AttributeFieldItemFinder.findFirst(item.fieldItems, START_DATE)
            val endDate = AttributeFieldItemFinder.findFirst(item.fieldItems, END_DATE)
            val major = AttributeFieldItemFinder.findFirst(item.fieldItems, MAJOR)
            binding.tvTime.text = (startDate?.displayValue ?: "") + " - " + (endDate?.displayValue ?: "")
            binding.tvMajor.text = major?.displayValue ?: ""

            if (binding.tvTime.text == " - ") {
                binding.tvTime.visibility = View.GONE
                binding.v.visibility = View.GONE
            } else {
                binding.tvTime.visibility = View.VISIBLE
            }

            if (binding.tvMajor.text.isNullOrEmpty()) {
                binding.v.visibility = View.GONE
                binding.tvMajor.visibility = View.GONE
            } else {
                binding.tvMajor.visibility = View.VISIBLE
            }

            binding.root.setOnClickListener {
                attributeData?.let { attr ->
                    EducationalActivity.start(requireActivity(), true, attr)
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
        binding.ivClose.setOnSingleClickListener { dismiss() }
        binding.clAddExperience.setOnSingleClickListener {
            EducationalActivity.start(requireActivity(), false, attributeData)
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
