package com.shiyuan.fastclaw.feature.information

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

        fun start(activity: Activity?, isEdit: Boolean = true, attributeData: AttributeArrayQueryData? = null, position: Int? = null) {
            activity ?: return
            val intent = Intent(activity, WorkActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
                attributeData?.let { putExtra(EXTRA_ATTRIBUTE_DATA, it) }
                position?.let { putExtra(EXTRA_INSTANCE_INDEX, it) }
            }
            activity.startActivity(intent)
        }
    }

    private var attributeData: AttributeArrayQueryData? = null

    private var position: Int = 0

    private val currentInstance: MutableList<AttributeFieldItem> = mutableListOf()

    private var isEdit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseIntent()
        currentInstance.clear()
        currentInstance.addAll(attributeData?.fieldTree?.getOrNull(this.position)?.fieldItems ?: emptyList())
        initView()
        observeEvents()
        if (isEdit) {
            bindEditData()
        }
        mRealVM.attributeSave.observe(this) {
            FlowEventBus.postSticky(Events.SuccessfulEvent())
            finish()
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
        position = intent?.getIntExtra(EXTRA_INSTANCE_INDEX, 0) ?: 0
    }

    private fun initView() {
        if (isEdit) {
            binding.tvTitle.text = getString(R.string.edit_working)
            binding.viewActionButtons.setSecondaryText(getString(R.string.del))
            binding.viewActionButtons.setPrimaryText(getString(R.string.complete))
            binding.viewActionButtons.setOnSecondaryClickListener {
                attributeData?.fieldTree?.removeAt(this.position)
                attributeData?.draft = true
                attributeData?.let { mRealVM.attributeSave(it) }
            }
            binding.viewActionButtons.setOnPrimaryClickListener {
                attributeData?.fieldTree?.getOrNull(this.position)?.fieldItems = currentInstance
                attributeData?.draft = true
                attributeData?.let { mRealVM.attributeSave(it) }
            }
        } else {
            binding.tvTitle.text = getString(R.string.add_working)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                attributeData?.fieldTree?.getOrNull(this.position)?.fieldItems = currentInstance
                attributeData?.draft = true
                attributeData?.let { mRealVM.attributeSave(it) }
            }
        }
        binding.vForm1.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_COMPANY_NAME)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }

        binding.vForm2.setOnSingleClickListener {
            JobSelectionActivity.start(this, false)
        }
        binding.tvStart.setOnClickListener {
            BirthYearMonthPickerDialog.show(
                manager = this.supportFragmentManager,
                onConfirm = { year, month -> onStartDateSelected(year, month) }
            )
        }
        binding.tvEnd.setOnClickListener {
            BirthYearMonthPickerDialog.show(
                manager = this.supportFragmentManager,
                onConfirm = { year, month -> onEndDateSelected(year, month) }
            )
        }
        binding.vForm3.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_WORK_CONTENT)
            if (item != null) {
                PublicDescriptionActivity.start(this, isEdit, item)
            }
        }
        binding.vForm5.setOnSingleClickListener {
            IndustrySelectionActivity.start(this, false)
        }
        binding.vForm6.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_COMPANY_SCALE)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        binding.vForm7.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_COMPANY_TYPE)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        binding.vForm8.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_TEAM_SIZE)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        binding.vForm9.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_LEAVING_REASON)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }

        binding.viewActionButtons.setSecondaryVisible(isEdit)
    }

    private fun onStartDateSelected(year: Int, month: String) {
        binding.tvStart.text = "$year-$month"
        val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_START_DATE)
        item?.let {
            val index = currentInstance.indexOf(it)
            if (index >= 0) {
                it.displayValue = "$year-$month"
                it.fieldValue = mapOf(
                    "value" to "$year-$month",
                    "displayText" to "$year-$month"
                )
                currentInstance[index] = it
            }
        }
    }

    private fun onEndDateSelected(year: Int, month: String) {
        binding.tvEnd.text = "$year-$month"
        val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_END_DATE)
        item?.let {
            val index = currentInstance.indexOf(it)
            if (index >= 0) {
                it.displayValue = "$year-$month"
                it.fieldValue = mapOf(
                    "value" to "$year-$month",
                    "displayText" to "$year-$month"
                )
                currentInstance[index] = it
            }
        }
    }

    private fun bindEditData() {
        val companyNameItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COMPANY_NAME)
        companyNameItem?.let {
            binding.vForm1.setValueText(it.displayValue)
        }

        val positionItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_POSITION)
        positionItem?.let {
            binding.vForm2.setValueText(it.displayValue)
        }

        val startDateItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_START_DATE)
        if (startDateItem != null) {
            binding.tvStart.text = startDateItem.displayValue
        }

        val endDateItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_END_DATE)
        if (endDateItem != null) {
            binding.tvEnd.text = endDateItem.displayValue
        }

        val workContentItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_WORK_CONTENT)
        workContentItem?.let {
            binding.vForm3.setValueText(it.displayValue)
        }

        val industryItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_INDUSTRY)
        industryItem?.let {
            binding.vForm5.setValueText(it.displayValue)
        }

        val companyScaleItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COMPANY_SCALE)
        companyScaleItem?.let {
            binding.vForm6.setValueText(it.displayValue)
        }

        val companyTypeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COMPANY_TYPE)
        companyTypeItem?.let {
            binding.vForm7.setValueText(it.displayValue)
        }

        val teamSizeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_TEAM_SIZE)
        teamSizeItem?.let {
            binding.vForm8.setValueText(it.displayValue)
        }

        val leavingReasonItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_LEAVING_REASON)
        leavingReasonItem?.let {
            binding.vForm9.setValueText(it.displayValue)
        }
    }

    private fun observeEvents() {
        observeEvents {
            on<Events.CommonAttributeFieldItemEvent> { event -> handleAttributeFieldItemEvent(event) }
            on<Events.CommonEnumItemEvent> { event -> handleCommonEnumItemEvent(event) }
        }
    }

    private fun handleAttributeFieldItemEvent(event: Events.CommonAttributeFieldItemEvent) {
        val fieldCode = event.attributeFieldItem?.fieldCode ?: return
        val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = fieldCode)
        if (item != null) {
            val index = currentInstance.indexOf(item)
            if (index >= 0) {
                currentInstance[index] = event.attributeFieldItem
            }
        }
        when (fieldCode) {
            FIELD_COMPANY_NAME -> binding.vForm1.setValueText(event.attributeFieldItem.displayValue)
            FIELD_POSITION -> binding.vForm2.setValueText(event.attributeFieldItem.displayValue)
            FIELD_WORK_CONTENT -> binding.vForm3.setValueText(event.attributeFieldItem.displayValue)
            FIELD_INDUSTRY -> binding.vForm5.setValueText(event.attributeFieldItem.displayValue)
            FIELD_COMPANY_SCALE -> binding.vForm6.setValueText(event.attributeFieldItem.displayValue)
            FIELD_COMPANY_TYPE -> binding.vForm7.setValueText(event.attributeFieldItem.displayValue)
            FIELD_TEAM_SIZE -> binding.vForm8.setValueText(event.attributeFieldItem.displayValue)
            FIELD_LEAVING_REASON -> binding.vForm9.setValueText(event.attributeFieldItem.displayValue)
        }
    }

    private fun handleCommonEnumItemEvent(event: Events.CommonEnumItemEvent) {
        when (event.commonEnumItems.fieldCode) {
            FIELD_POSITION -> {
                binding.vForm2.setValueText(event.commonEnumItems.positionName)
                updateFieldItemSimple(
                    attributeData = attributeData,
                    instanceIndex = position.toLong(),
                    fieldCode = FIELD_POSITION,
                    fieldName = "职位名称",
                    fieldValue = mapOf(
                        "value" to event.commonEnumItems.positionCode,
                        "displayText" to event.commonEnumItems.positionName
                    ),
                    displayValue = event.commonEnumItems.positionName ?: ""
                )
            }
            FIELD_INDUSTRY -> {
                binding.vForm5.setValueText(event.commonEnumItems.name)
                updateFieldItemSimple(
                    attributeData = attributeData,
                    instanceIndex = position.toLong(),
                    fieldCode = FIELD_INDUSTRY,
                    fieldName = "所属行业",
                    fieldValue = mapOf(
                        "value" to event.commonEnumItems.code,
                        "displayText" to event.commonEnumItems.name
                    ),
                    displayValue = event.commonEnumItems.name ?: ""
                )
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
