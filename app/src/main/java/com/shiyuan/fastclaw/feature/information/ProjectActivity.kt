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
import com.nhsd.basic.ext.showToast
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItemFinder
import com.shiyuan.fastclaw.databinding.ActivityProjectBinding
import com.shiyuan.fastclaw.feature.information.dialog.BirthYearMonthPickerDialog
import com.shiyuan.fastclaw.feature.information.dialog.DeleteConfirmDialog
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import java.util.Calendar

/**
 * 项目经历项编辑页：支持新增或编辑一条项目记录。
 * 字段：项目名称、项目角色、项目描述、起止时间、项目规模、技术栈、项目职责、项目成果、合作方。
 */
class ProjectActivity : BaseViewModelActivity<ActivityProjectBinding, InformationViewModel>() {
    companion object {
        // Intent 参数 key
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_ATTRIBUTE_DATA = "extra_attribute_data"
        private const val EXTRA_INSTANCE_INDEX = "extra_instance_index"

        // 字段编码
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
        private val REQUIRED_FIELDS = listOf(
            FIELD_PROJECT_NAME to R.string.project_name,
            FIELD_PROJECT_ROLE to R.string.project_role,
            FIELD_DESCRIPTION to R.string.project_desc
        )

        /**
         * 启动项目经历编辑页。
         */
        fun start(activity: Activity?, isEdit: Boolean = true, attributeData: AttributeArrayQueryData? = null, position: Int? = null) {
            activity ?: return
            val intent = Intent(activity, ProjectActivity::class.java).apply {
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
            binding.tvTitle.text = getString(R.string.edit_project)
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
            binding.tvTitle.text = getString(R.string.add_project)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                saveCurrentInstance()
            }
        }
        // 项目名称：自由填写
        binding.vForm1.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_PROJECT_NAME)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }

        // 项目角色：自由填写
        binding.vForm2.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_PROJECT_ROLE)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        // 项目描述：长文本填写
        binding.vForm3.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_DESCRIPTION)
            if (item != null) {
                PublicDescriptionActivity.start(this, isEdit, item,getString(R.string.project_responsibilities))
            }
        }
        // 起始时间：年月选择器
        binding.tvStart.setOnClickListener {
            val selectedDate = getFieldDate(FIELD_START_DATE)
            BirthYearMonthPickerDialog.show(
                manager = this.supportFragmentManager,
                title = "开始时间",
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
                title = "结束时间",
                selectedYear = selectedDate?.first,
                selectedMonth = selectedDate?.second,
                onConfirmWithTimestamp = { year, month, timestamp ->
                    onEndDateSelected(year, month, timestamp)
                }
            )
        }
        // 项目规模：自由填写
        binding.vForm5.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_PROJECT_SCALE)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        // 技术栈：自由填写
        binding.vForm6.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_TECH_STACK)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        // 项目职责：自由填写
        binding.vForm7.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_RESPONSIBILITIES)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        // 项目成果：自由填写
        binding.vForm8.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_RESULTS)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        // 合作方：自由填写
        binding.vForm10.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_COLLABORATORS)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
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
            "请填写：$emptyFieldName".showToast()
            return false
        }
        return true
    }

    /**
     * 处理项目起始时间选择结果。
     */
    private fun onStartDateSelected(year: Int, month: String, timestamp: Long) {
        val endTimestamp = getFieldDateTimestamp(FIELD_END_DATE)
        if (endTimestamp != null && timestamp > endTimestamp) {
            "起始时间不能大于结束时间".showToast()
            return
        }
        binding.tvStart.text = "$year-$month"
        updateDateField(FIELD_START_DATE, "$year-$month")
    }

    /**
     * 处理项目结束时间选择结果。
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
     * 更新年月字段的展示值和提交值。
     */
    private fun updateDateField(fieldCode: String, dateText: String) {
        val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = fieldCode)
        item?.let {
            item.displayValue = dateText
            item.fieldValue = mapOf(
                "value" to dateText,
                "displayText" to dateText
            )
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

    /**
     * 把已有数据回填到表单控件。
     */
    private fun bindEditData() {
        val projectNameItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_PROJECT_NAME)
        projectNameItem?.let {
            binding.vForm1.setValueText(it.displayValue)
        }

        val projectRoleItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_PROJECT_ROLE)
        projectRoleItem?.let {
            binding.vForm2.setValueText(it.displayValue)
        }

        val descriptionItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_DESCRIPTION)
        descriptionItem?.let {
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
            binding.vForm5.setValueText(it.displayValue)
        }

        val techStackItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_TECH_STACK)
        techStackItem?.let {
            binding.vForm6.setValueText(it.displayValue)
        }

        val responsibilitiesItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_RESPONSIBILITIES)
        responsibilitiesItem?.let {
            binding.vForm7.setValueText(it.displayValue)
        }

        val resultsItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_RESULTS)
        resultsItem?.let {
            binding.vForm8.setValueText(it.displayValue)
        }

        val collaboratorsItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COLLABORATORS)
        collaboratorsItem?.let {
            binding.vForm10.setValueText(it.displayValue)
        }
    }

    private fun observeEvents() {
        // 监听子页面自由填写 / 长文本回传事件
        observeEvents {
            on<Events.CommonAttributeFieldItemEvent> { event ->
                event.attributeFieldItem?.fieldCode?.let { fieldCode ->
                    val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = fieldCode)
                    item?.let {
                        val index = currentInstance.indexOf(item)
                        if (index >= 0) {
                            currentInstance[index] = event.attributeFieldItem
                        }
                    }
                    // 按字段编码回填到对应控件
                    when (fieldCode) {
                        FIELD_PROJECT_NAME -> {
                            binding.vForm1.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_PROJECT_ROLE -> {
                            binding.vForm2.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_DESCRIPTION -> {
                            binding.vForm3.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_PROJECT_SCALE -> {
                            binding.vForm5.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_TECH_STACK -> {
                            binding.vForm6.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_RESPONSIBILITIES -> {
                            binding.vForm7.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_RESULTS -> {
                            binding.vForm8.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_COLLABORATORS -> {
                            binding.vForm10.setValueText(event.attributeFieldItem.displayValue)
                        }
                    }
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
