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
import com.nhsd.basic.ext.showToast
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
import com.shiyuan.fastclaw.feature.information.dialog.DeleteConfirmDialog
import com.shiyuan.fastclaw.feature.information.dialog.SingleSelectDialog
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import kotlinx.serialization.json.Json
import java.util.Calendar

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
        private const val PRESENT_TEXT = "至今"
        private val REQUIRED_FIELDS = listOf(
            FIELD_COMPANY_NAME to R.string.company_name,
            FIELD_POSITION to R.string.job_title,
            FIELD_START_DATE to R.string.entry_time,
            FIELD_END_DATE to R.string.end_time,
            FIELD_WORK_CONTENT to R.string.job_content
        )

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
                DeleteConfirmDialog.show(
                    manager = supportFragmentManager,
                    onDelete = {
                        attributeData?.fieldTree?.removeAt(this.position)
                        attributeData?.draft = true
                        attributeData?.let { mRealVM.attributeSave(it) }
                    }
                )
            }
            // 完成编辑：写回字段项并保存
            binding.viewActionButtons.setOnPrimaryClickListener {
                saveCurrentInstance()
            }
        } else {
            // 新增模式：仅显示保存按钮
            binding.tvTitle.text = getString(R.string.add_working)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                saveCurrentInstance()
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
            val selectedDate = getFieldDate(FIELD_START_DATE)
            BirthYearMonthPickerDialog.show(
                manager = this.supportFragmentManager,
                title = "入职时间",
                selectedYear = selectedDate?.first,
                selectedMonth = selectedDate?.second,
                onConfirmWithTimestamp = { year, month, timestamp ->
                    onStartDateSelected(year, month, timestamp)
                }
            )
        }
        // 结束时间：年月选择器
        binding.tvEnd.setOnClickListener {
            val selectedDate = getFieldDate(FIELD_END_DATE)
            BirthYearMonthPickerDialog.show(
                manager = this.supportFragmentManager,
                title = "截至时间",
                selectedYear = selectedDate?.first,
                selectedMonth = selectedDate?.second,
                includePresentOption = true,
                selectedPresent = isFieldPresent(FIELD_END_DATE),
                onConfirmWithTimestamp = { year, month, timestamp ->
                    onEndDateSelected(year, month, timestamp)
                },
                onConfirmPresent = {
                    onEndDatePresentSelected()
                }
            )
        }
        // 工作内容：跳到长文本填写页
        binding.vForm3.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_WORK_CONTENT)
            if (item != null) {
                PublicDescriptionActivity.start(this, isEdit, item,getString(R.string.job_responsibilities))
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

    private fun saveCurrentInstance() {
        if (!validateRequiredFields()) return
        attributeData?.fieldTree?.getOrNull(this.position)?.fieldItems = currentInstance
        attributeData?.draft = true
        attributeData?.let { mRealVM.attributeSave(it) }
    }

    private fun validateRequiredFields(): Boolean {
        val emptyFieldName = REQUIRED_FIELDS.firstOrNull { (fieldCode, _) ->
            AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = fieldCode)?.displayValue.isNullOrBlank()
        }?.second?.let { getString(it) }
        if (emptyFieldName != null) {
            "请填写$emptyFieldName".showToast()
            return false
        }
        return true
    }

    /**
     * 处理起始时间选择结果。
     */
    private fun onStartDateSelected(year: Int, month: String, timestamp: Long) {
        val endTimestamp = getFieldDateTimestamp(FIELD_END_DATE)
        if (endTimestamp != null && timestamp > endTimestamp) {
            "起始时间不能大于结束时间".showToast()
            return
        }
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
    private fun onEndDateSelected(year: Int, month: String, timestamp: Long) {
        val startTimestamp = getFieldDateTimestamp(FIELD_START_DATE)
        if (startTimestamp != null && timestamp < startTimestamp) {
            "结束时间不能小于起始时间".showToast()
            return
        }
        binding.tvEnd.text = "$year-$month"
        updateDateField(FIELD_END_DATE, "$year-$month")
    }

    /**
     * 处理结束时间选择“至今”。
     */
    private fun onEndDatePresentSelected() {
        binding.tvEnd.text = PRESENT_TEXT
        updateDateField(FIELD_END_DATE, PRESENT_TEXT)
    }

    /**
     * 更新年月字段的展示值和提交值。
     */
    private fun updateDateField(fieldCode: String, dateText: String) {
        val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = fieldCode)
        item?.let {
            val index = currentInstance.indexOf(it)
            if (index >= 0) {
                it.displayValue = dateText
                it.fieldValue = mapOf(
                    "value" to if(dateText == PRESENT_TEXT) "PRESENT"  else  dateText,
                    "displayText" to dateText
                )
                currentInstance[index] = it
            }
        }
    }

    /**
     * 从字段中读取已选年月，并转换为该月 1 日 00:00:00 的本地毫秒时间戳。
     */
    private fun getFieldDateTimestamp(fieldCode: String): Long? {
        val (year, month) = getFieldDate(fieldCode) ?: return null
        return Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1, 0, 0, 0)
        }.timeInMillis
    }

    /**
     * 从字段展示值解析已选择的年月。
     */
    private fun getFieldDate(fieldCode: String): Pair<Int, Int>? {
        val displayValue = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode)?.displayValue
        val parts = displayValue?.split("-")?.takeIf { it.size == 2 } ?: return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
        return year to month
    }

    private fun isFieldPresent(fieldCode: String): Boolean {
        return AttributeFieldItemFinder.findFirst(currentInstance, fieldCode)?.displayValue == PRESENT_TEXT
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
