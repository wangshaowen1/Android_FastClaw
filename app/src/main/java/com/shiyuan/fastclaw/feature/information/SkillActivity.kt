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
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItemFinder
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

    private val fieldItemsMap = mutableMapOf<String, AttributeFieldItem>()

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
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_SKILL_NAME] ?: AttributeFieldItem(fieldCode = FIELD_SKILL_NAME, fieldName = "技能名称")
            } else {
                AttributeFieldItem(fieldCode = FIELD_SKILL_NAME, fieldName = "技能名称")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
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
        skillNameItem?.let {
            fieldItemsMap[FIELD_SKILL_NAME] = it
            binding.vForm1.setValueText(it.displayValue)
        }

        val proficiencyItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_PROFICIENCY)
        proficiencyItem?.let {
            fieldItemsMap[FIELD_PROFICIENCY] = it
            selectedProficiencyCode = it.fieldValue?.value
            binding.vForm2.setValueText(it.displayValue)
        }
    }

    private fun observeEvents() {
        observeEvents {
            on<Events.CommonAttributeFieldItemEvent> { event ->
                event.attributeFieldItem?.fieldCode?.let { fieldCode ->
                    attributeData?.fieldTree?.forEach { instance ->
                        val currentItems = instance.fieldItems?.toMutableList() ?: mutableListOf()
                        val index = currentItems.indexOfFirst { it.fieldCode == fieldCode }
                        if (index >= 0) {
                            currentItems[index] = event.attributeFieldItem
                        } else {
                            currentItems.add(event.attributeFieldItem)
                        }
                        instance.fieldItems = currentItems
                        return@forEach
                    }
                    when (fieldCode) {
                        FIELD_SKILL_NAME -> {
                            fieldItemsMap[FIELD_SKILL_NAME] = event.attributeFieldItem
                            binding.vForm1.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_PROFICIENCY -> fieldItemsMap[FIELD_PROFICIENCY] = event.attributeFieldItem
                    }
                }
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
