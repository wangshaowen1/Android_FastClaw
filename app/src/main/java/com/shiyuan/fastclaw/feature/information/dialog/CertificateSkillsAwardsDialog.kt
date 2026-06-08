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
import com.shiyuan.fastclaw.widget.decoration.SpaceDecoration
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.databinding.DialogCertificateSkillsAwardsBinding
import com.shiyuan.fastclaw.databinding.DialogEducationExperienceBinding
import com.shiyuan.fastclaw.databinding.DialogProjectExperienceBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class CertificateSkillsAwardsDialog : BaseBottomPushVMDialogFragment<DialogCertificateSkillsAwardsBinding, InformationViewModel>() {

    companion object {
        const val TAG = "WorkExperienceDialog"
        private var onAddClick: ((String) -> Unit)? = null
        private var onItemClick: ((ResumeData, String) -> Unit)? = null
        private var type: String = "Certificate"  // Certificate Skills Awards
         fun show(
            manager: FragmentManager,
            type: String,
            experiences: ArrayList<ResumeData>,
            onAddClick: ((String) -> Unit)? = null,
            onItemClick: ((ResumeData,String) -> Unit)? = null,
        ): CertificateSkillsAwardsDialog {
            this.onAddClick = onAddClick
            this.type = type
            this.onItemClick = onItemClick
            return CertificateSkillsAwardsDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_EXPERIENCES, experiences)
                }
                show(manager)
            }
        }

        private const val ARG_EXPERIENCES = "arg_experiences"
    }

    private val adapter by lazy {
        QuickAdapter<ItemCertificateSkillsAwardsVm>(R.layout.item_certificate_skills_awards, emptyList())
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
        if(type == "Skills"){
            binding.tvTitle.text = "技能"
            binding.tvAddText.text = "添加专业技能"
        }else if(type == "Awards"){
            binding.tvTitle.text = "奖项"
            binding.tvAddText.text = "添加奖项"
        }else{
            binding.tvTitle.text = "证书"
            binding.tvAddText.text = "添加资格证书"
        }
        binding.ivClose.setOnSingleClickListener { dismiss() }
        binding.clAddExperience.setOnSingleClickListener {
            dismiss()
            onAddClick?.invoke(type)
        }
    }

    private fun bindData() {
        val experiences = getExperiences()
        val items = experiences.map { data ->
            ItemCertificateSkillsAwardsVm(data).apply {
                onItemClick.value = {
                    dismiss()
                    CertificateSkillsAwardsDialog.onItemClick?.invoke(data,type)
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

class ItemCertificateSkillsAwardsVm(data: ResumeData?) : AppItemViewModel<ResumeData>(data) {

    fun onItemClick() {
        onItemClick.value?.invoke(null)
    }
}
