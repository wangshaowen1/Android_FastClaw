package com.shiyuan.fastclaw.feature.information

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.ActivityWorkBinding
import com.shiyuan.fastclaw.feature.information.dialog.BirthYearMonthPickerDialog
import com.shiyuan.fastclaw.feature.information.dialog.SingleSelectDialog
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import kotlinx.serialization.json.Json

/**
 * 工作经验项编辑页：支持新增或编辑一条工作经历。
 * 字段：公司名称、职位、起止时间、工作内容、行业、公司规模、公司类型、团队规模、离职原因。
 */
class WorkActivity : BaseViewModelActivity<ActivityWorkBinding, InformationViewModel>() {
    companion object {
        // Intent 参数 key
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_ATTRIBUTE_DATA = "extra_attribute_data"
        private const val EXTRA_INSTANCE_INDEX = "extra_instance_index"

        // 字段编码
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

        /**
         * 启动工作经历编辑页。
         */
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

    // 字段数据
    private var attributeData: AttributeArrayQueryData? = null

    // 当前编辑的字段实例索引
    private var position: Int = 0

    // 当前编辑实例的字段项集合
    private val currentInstance: MutableList<AttributeFieldItem> = mutableListOf()

    // 是否编辑模式
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
        // 保存成功：通知上游并关闭
        mRealVM.attributeSave.observe(this) {
            FlowEventBus.postSticky(Events.AttributeFieldSuccessfulEvent(attributeData))
            finish()
        }
    }

    private fun parseIntent() {
        isEdit = intent?.getBooleanExtra(EXTRA_IS_EDIT, true) ?: true
        @Suppress("DEPRECATION")
        // 兼容不同 Android 版本的 Parcelable 读取
        attributeData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_ATTRIBUTE_DATA, AttributeArrayQueryData::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_ATTRIBUTE_DATA)
        }
        position = intent?.getIntExtra(EXTRA_INSTANCE_INDEX, 0) ?: 0
    }

    private fun initView() {
        if (isEdit) {
            // 编辑模式：显示删除/完成按钮
            binding.tvTitle.text = getString(R.string.edit_working)
            binding.viewActionButtons.setSecondaryText(getString(R.string.del))
            binding.viewActionButtons.setPrimaryText(getString(R.string.complete))
            // 删除当前实例并保存
            binding.viewActionButtons.setOnSecondaryClickListener {
                attributeData?.fieldTree?.removeAt(this.position)
                attributeData?.draft = true
                attributeData?.let { mRealVM.attributeSave(it) }
            }
            // 完成编辑：写回字段项并保存
            binding.viewActionButtons.setOnPrimaryClickListener {
                attributeData?.fieldTree?.getOrNull(this.position)?.fieldItems = currentInstance
                attributeData?.draft = true
                attributeData?.let { mRealVM.attributeSave(it) }
            }
        } else {
            // 新增模式：仅显示保存按钮
            binding.tvTitle.text = getString(R.string.add_working)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                attributeData?.fieldTree?.getOrNull(this.position)?.fieldItems = currentInstance
                attributeData?.draft = true
                attributeData?.let { mRealVM.attributeSave(it) }
            }
        }
        // 公司名称：跳到自由填写页
        binding.vForm1.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_COMPANY_NAME)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }

        // 职位：跳到职位选择页（单选）
        binding.vForm2.setOnSingleClickListener {
            JobSelectionActivity.start(this, false)
        }
        // 起始时间：年月选择器
        binding.tvStart.setOnClickListener {
            BirthYearMonthPickerDialog.show(
                manager = this.supportFragmentManager,
                onConfirm = { year, month -> onStartDateSelected(year, month) }
            )
        }
        // 结束时间：年月选择器
        binding.tvEnd.setOnClickListener {
            BirthYearMonthPickerDialog.show(
                manager = this.supportFragmentManager,
                onConfirm = { year, month -> onEndDateSelected(year, month) }
            )
        }
        // 工作内容：跳到长文本填写页
        binding.vForm3.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_WORK_CONTENT)
            if (item != null) {
                PublicDescriptionActivity.start(this, isEdit, item)
            }
        }
        // 行业：跳到行业选择页
        binding.vForm5.setOnSingleClickListener {
            IndustrySelectionActivity.start(this, false)
        }
        // 公司规模：单选
        binding.vForm6.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance,fieldCode = FIELD_COMPANY_SCALE)
            val list = Json.decodeFromString<List<CommonEnumItem>>(item?.questionOptionsJson?:"[]")
            SingleSelectDialog.show(this.supportFragmentManager,"公司规模",list){  data->
                binding.vForm6.setValueText(data.name)
                item?.let {
                    item.displayValue =  data.name
                    item.fieldValue = mapOf(
                        "value" to data.code ,
                        "displayText" to data.name
                    )
                }
            }
        }
        // 公司类型：单选
        binding.vForm7.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance,fieldCode = FIELD_COMPANY_TYPE)
            val list = Json.decodeFromString<List<CommonEnumItem>>(item?.questionOptionsJson?:"[]")
            SingleSelectDialog.show(this.supportFragmentManager,"公司类型",list){  data->
                binding.vForm7.setValueText(data.name)
                item?.let {
                    item.displayValue =  data.name
                    item.fieldValue = mapOf(
                        "value" to data.code ,
                        "displayText" to data.name
                    )
                }
            }
        }
        // 团队规模：自由填写
        binding.vForm8.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_TEAM_SIZE)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        // 离职原因：单选
        binding.vForm9.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance,fieldCode = FIELD_LEAVING_REASON)
            val list = Json.decodeFromString<List<CommonEnumItem>>(item?.questionOptionsJson?:"[]")
            SingleSelectDialog.show(this.supportFragmentManager,"公司类型",list){  data->
                binding.vForm9.setValueText(data.name)
                item?.let {
                    item.displayValue =  data.name
                    item.fieldValue = mapOf(
                        "value" to data.code ,
                        "displayText" to data.name
                    )
                }
            }
        }

        // 仅编辑模式显示删除按钮
        binding.viewActionButtons.setSecondaryVisible(isEdit)
    }

    /**
     * 处理起始时间选择结果。
     */
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

    /**
     * 处理结束时间选择结果。
     */
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

    /**
     * 把已有数据回填到表单控件。
     */
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
        // 监听子页面回传的事件
        observeEvents {
            on<Events.CommonAttributeFieldItemEvent> { event -> handleAttributeFieldItemEvent(event) }
            on<Events.CommonEnumItemEvent> { event -> handleCommonEnumItemEvent(event) }
        }
    }

    /**
     * 处理自由填写 / 长文本回传事件。
     */
    private fun handleAttributeFieldItemEvent(event: Events.CommonAttributeFieldItemEvent) {
        val fieldCode = event.attributeFieldItem?.fieldCode ?: return
        val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = fieldCode)
        if (item != null) {
            val index = currentInstance.indexOf(item)
            if (index >= 0) {
                currentInstance[index] = event.attributeFieldItem
            }
        }
        // 按字段编码回填到对应控件
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

    /**
     * 处理单选项选择回传事件。
     */
    private fun handleCommonEnumItemEvent(event: Events.CommonEnumItemEvent) {
        when (event.commonEnumItems.fieldCode) {
            FIELD_POSITION -> {
                // 职位回传
                binding.vForm2.setValueText(event.commonEnumItems.positionName)
                val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_POSITION)
                item?.let {
                    item.displayValue =  event.commonEnumItems.positionName ?: ""
                    item.fieldValue = mapOf(
                        "value" to event.commonEnumItems.positionName,
                        "displayText" to event.commonEnumItems.positionName
                    )
                }
            }
            FIELD_INDUSTRY -> {
                // 行业回传
                binding.vForm5.setValueText(event.commonEnumItems.name)
                val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_INDUSTRY)
                item?.let {
                    item.displayValue =  event.commonEnumItems.name ?: ""
                    item.fieldValue = mapOf(
                        "value" to event.commonEnumItems.name,
                        "displayText" to event.commonEnumItems.name
                    )
                }
            }
        }
    }

    override fun setTranslucentStatus() {
        // 设置沉浸式状态栏
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

}
