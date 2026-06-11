package com.shiyuan.fastclaw.feature.information

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItemFinder
import com.shiyuan.fastclaw.core.network.apis.response.CommonItems
import com.shiyuan.fastclaw.databinding.ActivitySkillBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class SkillActivity : BaseViewModelActivity<ActivitySkillBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_ATTRIBUTE_DATA = "extra_attribute_data"
        private const val EXTRA_INSTANCE_INDEX = "extra_instance_index"

        private const val FIELD_SKILL_NAME = "skillName"
        private const val FIELD_PROFICIENCY = "proficiency"

        fun start(activity: Activity?, isEdit: Boolean = true, attributeData: AttributeArrayQueryData? = null, instanceIndex: Long? = null) {
            activity ?: return
            val intent = Intent(activity, SkillActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
                attributeData?.let { putExtra(EXTRA_ATTRIBUTE_DATA, it) }
                instanceIndex?.let { putExtra(EXTRA_INSTANCE_INDEX, it) }
            }
            activity.startActivity(intent)
        }
    }

    private var attributeData: AttributeArrayQueryData? = null
    private var instanceIndex: Long? = null
    private var selectedProficiencyCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseIntent()
        initView()
        observeEvents()
        if (isEdit) {
            bindEditData()
        }
    }

    private fun parseIntent() {
        isEdit = intent?.getBooleanExtra(EXTRA_IS_EDIT, true) ?: true
        @Suppress("DEPRECATION")
        attributeData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_ATTRIBUTE_DATA, AttributeArrayQueryData::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_ATTRIBUTE_DATA)
        }
        instanceIndex = intent?.getLongExtra(EXTRA_INSTANCE_INDEX, -1L)?.takeIf { it != -1L }
    }

    private var isEdit = true

    private fun initView() {
        if (isEdit) {
            binding.tvTitle.text = getString(R.string.edit_skill)
            binding.viewActionButtons.setSecondaryText(getString(R.string.del))
            binding.viewActionButtons.setPrimaryText(getString(R.string.complete))
            binding.viewActionButtons.setOnSecondaryClickListener {
                finish()
            }
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        } else {
            binding.tvTitle.text = getString(R.string.add_skill)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        }
        binding.vForm1.setOnSingleClickListener {
            PublicFreeFillInActivity.start(this, isEdit, CommonItems(fieldName = "技能名称"))
        }

        binding.vForm2.setOnSingleClickListener {

        }
        binding.viewActionButtons.setSecondaryVisible(isEdit)
    }

    private fun bindEditData() {
        val fieldTree = attributeData?.fieldTree ?: return
        val currentInstance = if (instanceIndex != null) {
            fieldTree.find { it.instanceIndex == instanceIndex }
        } else {
            fieldTree.firstOrNull()
        } ?: return

        val skillNameItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_SKILL_NAME)
        skillNameItem?.displayValue?.let {
            binding.vForm1.setData(CommonItems(fieldName = "技能名称", fieldValue = it))
        }

        val proficiencyItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_PROFICIENCY)
        proficiencyItem?.displayValue?.let {
            selectedProficiencyCode = proficiencyItem.fieldValue?.value
            binding.vForm2.setData(CommonItems(fieldName = "熟练度", fieldValue = it))
        }
    }

    private fun observeEvents() {
        observeEvents {
            on<Events.CommonItemsEvent> { event ->
                binding.vForm1.setData(event.commonItems)
            }
        }
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

}
