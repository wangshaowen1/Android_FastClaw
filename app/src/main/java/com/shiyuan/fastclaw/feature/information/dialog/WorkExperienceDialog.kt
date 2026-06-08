package com.shiyuan.fastclaw.feature.information.dialog

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.base.item.AppItemViewModel
import com.shiyuan.fastclaw.common.adapter.QuickAdapter
import com.shiyuan.fastclaw.core.network.apis.entity.ResumeData
import com.shiyuan.fastclaw.databinding.DialogWorkExperienceBinding
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class WorkExperienceDialog : BaseBottomPushVMDialogFragment<DialogWorkExperienceBinding, InformationViewModel>() {

    companion object {
        const val TAG = "WorkExperienceDialog"
        private var onAddClick: (() -> Unit)? = null
        private var onItemClick: ((ResumeData) -> Unit)? = null

        fun show(
            manager: FragmentManager,
            experiences: ArrayList<ResumeData>,
            onAddClick: (() -> Unit)? = null,
            onItemClick: ((ResumeData) -> Unit)? = null,
        ): WorkExperienceDialog {
            this.onAddClick = onAddClick
            this.onItemClick = onItemClick
            return WorkExperienceDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_EXPERIENCES, experiences)
                }
                show(manager)
            }
        }

        private const val ARG_EXPERIENCES = "arg_experiences"
    }

    private val adapter by lazy {
        QuickAdapter<ItemWorkExperienceVm>(R.layout.item_work_experience, emptyList())
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
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
        binding.rvExperience.adapter = adapter
    }

    private fun initView() {
        binding.ivClose.setOnSingleClickListener { dismiss() }
        binding.clAddExperience.setOnSingleClickListener {
            dismiss()
            onAddClick?.invoke()
        }
    }

    private fun bindData() {
        val experiences = getExperiences()
        val items = experiences.map { data ->
            ItemWorkExperienceVm(data).apply {
                onItemClick.value = {
                    dismiss()
                       WorkExperienceDialog.onItemClick?.invoke(data)
                }
            }
        }
        adapter.submitList(items)
    }

    @Suppress("DEPRECATION")
    private fun getExperiences(): ArrayList<ResumeData> {
        val bundle = arguments ?: return arrayListOf()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelableArrayList(ARG_EXPERIENCES, ResumeData::class.java) ?: arrayListOf()
        } else {
            bundle.getParcelableArrayList(ARG_EXPERIENCES) ?: arrayListOf()
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
    }
}

class ItemWorkExperienceVm(data: ResumeData?) : AppItemViewModel<ResumeData>(data) {

    fun onItemClick() {
        onItemClick.value?.invoke(null)
    }
}
