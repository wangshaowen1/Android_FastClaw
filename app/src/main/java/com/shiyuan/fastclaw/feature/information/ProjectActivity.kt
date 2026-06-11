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
import com.shiyuan.fastclaw.databinding.ActivityProjectBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class ProjectActivity : BaseViewModelActivity<ActivityProjectBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_ATTRIBUTE_DATA = "extra_attribute_data"
        private const val EXTRA_INSTANCE_INDEX = "extra_instance_index"

        private const val FIELD_PROJECT_NAME = "projectName"
        private const val FIELD_PROJECT_ROLE = "projectRole"
        private const val FIELD_PROJECT_SCALE = "projectScale"
        private const val FIELD_TECH_STACK = "techStack"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_RESPONSIBILITIES = "responsibilities"
        private const val FIELD_RESULTS = "results"
        private const val FIELD_COLLABORATORS = "collaborators"
        private const val FIELD_START_DATE = "startDate"
        private const val FIELD_END_DATE = "endDate"

        fun start(activity: Activity?, isEdit: Boolean = true, attributeData: AttributeArrayQueryData? = null, instanceIndex: Long? = null) {
            activity ?: return
            val intent = Intent(activity, ProjectActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
                attributeData?.let { putExtra(EXTRA_ATTRIBUTE_DATA, it) }
                instanceIndex?.let { putExtra(EXTRA_INSTANCE_INDEX, it) }
            }
            activity.startActivity(intent)
        }
    }

    private var attributeData: AttributeArrayQueryData? = null
    private var instanceIndex: Long? = null

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
            binding.tvTitle.text = getString(R.string.edit_project)
            binding.viewActionButtons.setSecondaryText(getString(R.string.del))
            binding.viewActionButtons.setPrimaryText(getString(R.string.complete))
            binding.viewActionButtons.setOnSecondaryClickListener {
                finish()
            }
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        } else {
            binding.tvTitle.text = getString(R.string.add_project)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        }
        binding.vForm1.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_PROJECT_NAME] ?: AttributeFieldItem(fieldCode = FIELD_PROJECT_NAME, fieldName = "项目名称")
            } else {
                AttributeFieldItem(fieldCode = FIELD_PROJECT_NAME, fieldName = "项目名称")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }

        binding.vForm2.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_PROJECT_ROLE] ?: AttributeFieldItem(fieldCode = FIELD_PROJECT_ROLE, fieldName = "项目角色")
            } else {
                AttributeFieldItem(fieldCode = FIELD_PROJECT_ROLE, fieldName = "项目角色")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm3.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_DESCRIPTION] ?: AttributeFieldItem(fieldCode = FIELD_DESCRIPTION, fieldName = "项目描述")
            } else {
                AttributeFieldItem(fieldCode = FIELD_DESCRIPTION, fieldName = "项目描述")
            }
            PublicDescriptionActivity.start(this, isEdit, fieldItem)
        }
        binding.tvStart.setOnClickListener {

        }
        binding.tvEnd.setOnClickListener {

        }
        binding.vForm5.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_PROJECT_SCALE] ?: AttributeFieldItem(fieldCode = FIELD_PROJECT_SCALE, fieldName = "项目规模")
            } else {
                AttributeFieldItem(fieldCode = FIELD_PROJECT_SCALE, fieldName = "项目规模")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm6.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_TECH_STACK] ?: AttributeFieldItem(fieldCode = FIELD_TECH_STACK, fieldName = "技术栈")
            } else {
                AttributeFieldItem(fieldCode = FIELD_TECH_STACK, fieldName = "技术栈")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm7.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_RESPONSIBILITIES] ?: AttributeFieldItem(fieldCode = FIELD_RESPONSIBILITIES, fieldName = "项目职责")
            } else {
                AttributeFieldItem(fieldCode = FIELD_RESPONSIBILITIES, fieldName = "项目职责")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm8.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_RESULTS] ?: AttributeFieldItem(fieldCode = FIELD_RESULTS, fieldName = "项目成果")
            } else {
                AttributeFieldItem(fieldCode = FIELD_RESULTS, fieldName = "项目成果")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm9.setOnSingleClickListener {

        }
        binding.vForm10.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_COLLABORATORS] ?: AttributeFieldItem(fieldCode = FIELD_COLLABORATORS, fieldName = "协作部门")
            } else {
                AttributeFieldItem(fieldCode = FIELD_COLLABORATORS, fieldName = "协作部门")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
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

        val projectNameItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_PROJECT_NAME)
        projectNameItem?.let {
            fieldItemsMap[FIELD_PROJECT_NAME] = it
            binding.vForm1.setValueText(it.displayValue)
        }

        val projectRoleItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_PROJECT_ROLE)
        projectRoleItem?.let {
            fieldItemsMap[FIELD_PROJECT_ROLE] = it
            binding.vForm2.setValueText(it.displayValue)
        }

        val descriptionItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_DESCRIPTION)
        descriptionItem?.let {
            fieldItemsMap[FIELD_DESCRIPTION] = it
            binding.vForm3.setValueText(it.displayValue)
        }

        val startDateItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_START_DATE)
        startDateItem?.displayValue?.let {
            binding.tvStart.text = it
        }

        val endDateItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_END_DATE)
        endDateItem?.displayValue?.let {
            binding.tvEnd.text = it
        }

        val projectScaleItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_PROJECT_SCALE)
        projectScaleItem?.let {
            fieldItemsMap[FIELD_PROJECT_SCALE] = it
            binding.vForm5.setValueText(it.displayValue)
        }

        val techStackItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_TECH_STACK)
        techStackItem?.let {
            fieldItemsMap[FIELD_TECH_STACK] = it
            binding.vForm6.setValueText(it.displayValue)
        }

        val responsibilitiesItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_RESPONSIBILITIES)
        responsibilitiesItem?.let {
            fieldItemsMap[FIELD_RESPONSIBILITIES] = it
            binding.vForm7.setValueText(it.displayValue)
        }

        val resultsItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_RESULTS)
        resultsItem?.let {
            fieldItemsMap[FIELD_RESULTS] = it
            binding.vForm8.setValueText(it.displayValue)
        }

        val collaboratorsItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COLLABORATORS)
        collaboratorsItem?.let {
            fieldItemsMap[FIELD_COLLABORATORS] = it
            binding.vForm10.setValueText(it.displayValue)
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
                        FIELD_PROJECT_NAME -> {
                            fieldItemsMap[FIELD_PROJECT_NAME] = event.attributeFieldItem
                            binding.vForm1.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_PROJECT_ROLE -> {
                            fieldItemsMap[FIELD_PROJECT_ROLE] = event.attributeFieldItem
                            binding.vForm2.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_DESCRIPTION -> {
                            fieldItemsMap[FIELD_DESCRIPTION] = event.attributeFieldItem
                            binding.vForm3.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_PROJECT_SCALE -> {
                            fieldItemsMap[FIELD_PROJECT_SCALE] = event.attributeFieldItem
                            binding.vForm5.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_TECH_STACK -> {
                            fieldItemsMap[FIELD_TECH_STACK] = event.attributeFieldItem
                            binding.vForm6.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_RESPONSIBILITIES -> {
                            fieldItemsMap[FIELD_RESPONSIBILITIES] = event.attributeFieldItem
                            binding.vForm7.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_RESULTS -> {
                            fieldItemsMap[FIELD_RESULTS] = event.attributeFieldItem
                            binding.vForm8.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_COLLABORATORS -> {
                            fieldItemsMap[FIELD_COLLABORATORS] = event.attributeFieldItem
                            binding.vForm10.setValueText(event.attributeFieldItem.displayValue)
                        }
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
