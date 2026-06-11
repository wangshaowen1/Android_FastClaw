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
            PublicFreeFillInActivity.start(this, isEdit, CommonItems(fieldName = "项目名称"))
        }

        binding.vForm2.setOnSingleClickListener {

        }
        binding.vForm3.setOnSingleClickListener {

        }
        binding.tvStart.setOnClickListener {

        }
        binding.tvEnd.setOnClickListener {

        }
        binding.vForm5.setOnSingleClickListener {

        }
        binding.vForm6.setOnSingleClickListener {

        }
        binding.vForm7.setOnSingleClickListener {

        }
        binding.vForm8.setOnSingleClickListener {

        }
        binding.vForm9.setOnSingleClickListener {

        }
        binding.vForm10.setOnSingleClickListener {

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
        projectNameItem?.displayValue?.let {
            binding.vForm1.setData(CommonItems(fieldName = "项目名称", fieldValue = it))
        }

        val projectRoleItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_PROJECT_ROLE)
        projectRoleItem?.displayValue?.let {
            binding.vForm2.setData(CommonItems(fieldName = "项目角色", fieldValue = it))
        }

        val descriptionItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_DESCRIPTION)
        descriptionItem?.displayValue?.let {
            binding.vForm3.setData(CommonItems(fieldName = "项目描述", fieldValue = it))
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
        projectScaleItem?.displayValue?.let {
            binding.vForm5.setData(CommonItems(fieldName = "项目规模", fieldValue = it))
        }

        val techStackItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_TECH_STACK)
        techStackItem?.displayValue?.let {
            binding.vForm6.setData(CommonItems(fieldName = "技术栈", fieldValue = it))
        }

        val responsibilitiesItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_RESPONSIBILITIES)
        responsibilitiesItem?.displayValue?.let {
            binding.vForm7.setData(CommonItems(fieldName = "项目职责", fieldValue = it))
        }

        val resultsItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_RESULTS)
        resultsItem?.displayValue?.let {
            binding.vForm8.setData(CommonItems(fieldName = "项目成果", fieldValue = it))
        }

        val collaboratorsItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COLLABORATORS)
        collaboratorsItem?.displayValue?.let {
            binding.vForm10.setData(CommonItems(fieldName = "协作部门", fieldValue = it))
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
