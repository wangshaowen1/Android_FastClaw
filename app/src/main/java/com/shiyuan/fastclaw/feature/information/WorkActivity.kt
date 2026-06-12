package com.shiyuan.fastclaw.feature.information

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.setOnSingleClickListener
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeDataHelper
import com.shiyuan.fastclaw.core.network.apis.response.AttributeDataHelper.updateFieldItemDirect
import com.shiyuan.fastclaw.core.network.apis.response.AttributeDataHelper.updateFieldItemSimple
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItemFinder
import com.shiyuan.fastclaw.databinding.ActivityWorkBinding
import com.shiyuan.fastclaw.feature.information.dialog.BirthYearMonthPickerDialog
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class WorkActivity : BaseViewModelActivity<ActivityWorkBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_ATTRIBUTE_DATA = "extra_attribute_data"
        private const val EXTRA_INSTANCE_INDEX = "extra_instance_index"

        private const val FIELD_COMPANY_NAME = "companyName"
        private const val FIELD_POSITION = "position"
        private const val FIELD_INDUSTRY = "industry"
        private const val FIELD_COMPANY_SCALE = "companyScale"
        private const val FIELD_COMPANY_TYPE = "companyType"
        private const val FIELD_TEAM_SIZE = "teamSize"
        private const val FIELD_WORK_CONTENT = "workContent"
        private const val FIELD_LEAVING_REASON = "leavingReason"
        private const val FIELD_START_DATE = "startDate"
        private const val FIELD_END_DATE = "endDate"

        fun start(activity: Activity?, isEdit: Boolean = true, attributeData: AttributeArrayQueryData? = null, instanceIndex: Long? = null) {
            activity ?: return
            val intent = Intent(activity, WorkActivity::class.java).apply {
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

    private fun getOrCreateFieldItem(fieldCode: String, fieldName: String): AttributeFieldItem {
        return fieldItemsMap.getOrPut(fieldCode) {
            fieldItemsMap[fieldCode] ?: AttributeFieldItem(fieldCode = fieldCode, fieldName = fieldName)
        }
    }

    private fun initView() {
        if (isEdit) {
            binding.tvTitle.text = getString(R.string.edit_working)
            binding.viewActionButtons.setSecondaryText(getString(R.string.del))
            binding.viewActionButtons.setPrimaryText(getString(R.string.complete))
            binding.viewActionButtons.setOnSecondaryClickListener {
                finish()
            }
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        } else {
            binding.tvTitle.text = getString(R.string.add_working)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        }
        binding.vForm1.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_COMPANY_NAME] ?: AttributeFieldItem(fieldCode = FIELD_COMPANY_NAME, fieldName = "公司名称")
            } else {
                AttributeFieldItem(fieldCode = FIELD_COMPANY_NAME, fieldName = "公司名称")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }

        binding.vForm2.setOnSingleClickListener {
           /* val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_POSITION] ?: AttributeFieldItem(fieldCode = FIELD_POSITION, fieldName = "职位名称")
            } else {
                AttributeFieldItem(fieldCode = FIELD_POSITION, fieldName = "职位名称")
            }*/
          //  PublicFreeFillInActivity.start(this, isEdit, fieldItem)

            JobSelectionActivity.start(this, false)
        }
        binding.tvStart.setOnClickListener {

            BirthYearMonthPickerDialog.show(this.supportFragmentManager){ year,month->
                binding.tvStart.text = "$year-$month"
                updateFieldItemDirect(
                    attributeData,
                    instanceIndex =  instanceIndex ?: 0,
                    newItem = AttributeFieldItem(fieldCode = FIELD_START_DATE, fieldName = "开始时间",displayValue = "$year-$month")
                )
            }
        }
        binding.tvEnd.setOnClickListener {
            BirthYearMonthPickerDialog.show(this.supportFragmentManager){ year,month->
                binding.tvStart.text = "$year-$month"
                updateFieldItemDirect(
                    attributeData,
                    instanceIndex =  instanceIndex ?: 0,
                    newItem =  AttributeFieldItem(fieldCode = FIELD_END_DATE, fieldName = "结束时间",displayValue = "$year-$month")
                )
            }
        }
        binding.vForm3.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_WORK_CONTENT] ?: AttributeFieldItem(fieldCode = FIELD_WORK_CONTENT, fieldName = "工作内容")
            } else {
                AttributeFieldItem(fieldCode = FIELD_WORK_CONTENT, fieldName = "工作内容")
            }
            PublicDescriptionActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm5.setOnSingleClickListener {
           /* JobSelectionActivity.start(this, false)

            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_INDUSTRY] ?: AttributeFieldItem(fieldCode = FIELD_INDUSTRY, fieldName = "所属行业")
            } else {
                AttributeFieldItem(fieldCode = FIELD_INDUSTRY, fieldName = "所属行业")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)*/
            IndustrySelectionActivity.start(this, false)
        }
        binding.vForm6.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_COMPANY_SCALE] ?: AttributeFieldItem(fieldCode = FIELD_COMPANY_SCALE, fieldName = "公司规模")
            } else {
                AttributeFieldItem(fieldCode = FIELD_COMPANY_SCALE, fieldName = "公司规模")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm7.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_COMPANY_TYPE] ?: AttributeFieldItem(fieldCode = FIELD_COMPANY_TYPE, fieldName = "公司类型")
            } else {
                AttributeFieldItem(fieldCode = FIELD_COMPANY_TYPE, fieldName = "公司类型")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm8.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_TEAM_SIZE] ?: AttributeFieldItem(fieldCode = FIELD_TEAM_SIZE, fieldName = "团队规模")
            } else {
                AttributeFieldItem(fieldCode = FIELD_TEAM_SIZE, fieldName = "团队规模")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm9.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_LEAVING_REASON] ?: AttributeFieldItem(fieldCode = FIELD_LEAVING_REASON, fieldName = "离职原因")
            } else {
                AttributeFieldItem(fieldCode = FIELD_LEAVING_REASON, fieldName = "离职原因")
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

        val companyNameItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COMPANY_NAME)
        companyNameItem?.let {
            fieldItemsMap[FIELD_COMPANY_NAME] = it
            binding.vForm1.setValueText(it.displayValue)
        }

        val positionItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_POSITION)
        positionItem?.let {
            fieldItemsMap[FIELD_POSITION] = it
            binding.vForm2.setValueText(it.displayValue)
        }

        val startDateItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_START_DATE)
        startDateItem?.displayValue?.let {
            binding.tvStart.text = it
        }

        val endDateItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_END_DATE)
        endDateItem?.displayValue?.let {
            binding.tvEnd.text = it
        }

        val workContentItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_WORK_CONTENT)
        workContentItem?.let {
            fieldItemsMap[FIELD_WORK_CONTENT] = it
            binding.vForm3.setValueText(it.displayValue)
        }

        val industryItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_INDUSTRY)
        industryItem?.let {
            fieldItemsMap[FIELD_INDUSTRY] = it
            binding.vForm5.setValueText(it.displayValue)
        }

        val companyScaleItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COMPANY_SCALE)
        companyScaleItem?.let {
            fieldItemsMap[FIELD_COMPANY_SCALE] = it
            binding.vForm6.setValueText(it.displayValue)
        }

        val companyTypeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COMPANY_TYPE)
        companyTypeItem?.let {
            fieldItemsMap[FIELD_COMPANY_TYPE] = it
            binding.vForm7.setValueText(it.displayValue)
        }

        val teamSizeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_TEAM_SIZE)
        teamSizeItem?.let {
            fieldItemsMap[FIELD_TEAM_SIZE] = it
            binding.vForm8.setValueText(it.displayValue)
        }

        val leavingReasonItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_LEAVING_REASON)
        leavingReasonItem?.let {
            fieldItemsMap[FIELD_LEAVING_REASON] = it
            binding.vForm9.setValueText(it.displayValue)
        }
    }

    private fun observeEvents() {

        observeEvents {
            on<Events.CommonAttributeFieldItemEvent> { event ->
               updateFieldItemDirect(
                    attributeData,
                    instanceIndex =  instanceIndex ?: 0,
                    newItem = event.attributeFieldItem
                )
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
                        FIELD_COMPANY_NAME -> {
                            fieldItemsMap[FIELD_COMPANY_NAME] = event.attributeFieldItem
                            binding.vForm1.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_POSITION -> {
                            fieldItemsMap[FIELD_POSITION] = event.attributeFieldItem
                            binding.vForm2.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_WORK_CONTENT -> {
                            fieldItemsMap[FIELD_WORK_CONTENT] = event.attributeFieldItem
                            binding.vForm3.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_INDUSTRY -> {
                            fieldItemsMap[FIELD_INDUSTRY] = event.attributeFieldItem
                            binding.vForm5.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_COMPANY_SCALE -> {
                            fieldItemsMap[FIELD_COMPANY_SCALE] = event.attributeFieldItem
                            binding.vForm6.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_COMPANY_TYPE -> {
                            fieldItemsMap[FIELD_COMPANY_TYPE] = event.attributeFieldItem
                            binding.vForm7.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_TEAM_SIZE -> {
                            fieldItemsMap[FIELD_TEAM_SIZE] = event.attributeFieldItem
                            binding.vForm8.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_LEAVING_REASON -> {
                            fieldItemsMap[FIELD_LEAVING_REASON] = event.attributeFieldItem
                            binding.vForm9.setValueText(event.attributeFieldItem.displayValue)
                        }
                    }
                }
            }
            on<Events.CommonEnumItemEvent>{ event ->
                when (event.commonEnumItems.fieldCode) {
                    FIELD_POSITION -> {
                        binding.vForm2.setValueText(event.commonEnumItems.positionName)
                        updateFieldItemSimple(
                            attributeData = attributeData,
                            instanceIndex = instanceIndex ?: 0,
                            fieldCode = FIELD_POSITION,
                            fieldName = "职位名称",
                            fieldValue = mapOf(
                                "value" to event.commonEnumItems.positionCode ,
                                "displayText" to event.commonEnumItems.positionName
                            ),
                            displayValue = event.commonEnumItems.positionName ?:""
                        )
                    }
                    FIELD_INDUSTRY -> {
                    binding.vForm5.setValueText(event.commonEnumItems.name)
                    updateFieldItemSimple(
                        attributeData = attributeData,
                        instanceIndex = instanceIndex ?: 0,
                        fieldCode = FIELD_INDUSTRY,
                        fieldName = "所属行业",
                        fieldValue = mapOf(
                            "value" to event.commonEnumItems.code ,
                            "displayText" to event.commonEnumItems.name
                        ),
                        displayValue = event.commonEnumItems.name ?:""
                    )
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

