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
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItemFinder
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumTreeUtil.convertCodeBooleanToString
import com.shiyuan.fastclaw.databinding.ActivityEducationalBinding
import com.shiyuan.fastclaw.feature.information.dialog.BirthYearMonthPickerDialog
import com.shiyuan.fastclaw.feature.information.dialog.SingleSelectDialog
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * 教育经历项编辑页：支持新增或编辑一条教育经历。
 * 字段：学校、学历、专业、起止时间、是否全日制、院校性质、GPA、排名荣誉、主修课程、描述。
 */
class EducationalActivity : BaseViewModelActivity<ActivityEducationalBinding, InformationViewModel>() {
    companion object {
        // Intent 参数 key
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_ATTRIBUTE_DATA = "extra_attribute_data"
        private const val EXTRA_INSTANCE_INDEX = "extra_instance_index"

        // 字段编码
        private const val FIELD_SCHOOL_NAME = "schoolName"
        private const val FIELD_MAJOR = "major"
        private const val FIELD_GPA = "gpa"
        private const val FIELD_RANKING_OR_HONORS = "rankingOrHonors"
        private const val FIELD_MAIN_COURSES = "mainCourses"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_DEGREE = "degree"
        private const val FIELD_FULL_TIME = "fullTime"
        private const val FIELD_SCHOOL_TYPE = "schoolType"
        private const val FIELD_START_DATE = "startDate"
        private const val FIELD_END_DATE = "endDate"

        /**
         * 启动教育经历编辑页。
         */
        fun start(activity: Activity?, isEdit: Boolean = true, attributeData: AttributeArrayQueryData? = null, position: Int? = null) {
            activity ?: return
            val intent = Intent(activity, EducationalActivity::class.java).apply {
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
            binding.tvTitle.text = getString(R.string.edit_educational)
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
            binding.tvTitle.text = getString(R.string.add_educational)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                attributeData?.fieldTree?.getOrNull(this.position)?.fieldItems = currentInstance
                attributeData?.draft = true
                attributeData?.let { mRealVM.attributeSave(it) }
            }
        }
        // 学校名称：自由填写
        binding.vForm1.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_SCHOOL_NAME)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }

        // 学历：单选
        binding.vForm2.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance,fieldCode = FIELD_DEGREE)
            val list = Json.decodeFromString<List<CommonEnumItem>>(item?.questionOptionsJson?:"[]")
            SingleSelectDialog.show(this.supportFragmentManager,"学历",list){  data->
                binding.vForm2.setValueText(data.name)
                item?.let {
                    item.displayValue =  data.name
                    item.fieldValue = mapOf(
                        "value" to data.code ,
                        "displayText" to data.name
                    )
                }
            }
        }
        // 专业：自由填写
        binding.vForm3.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_MAJOR)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
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
                onConfirmWithTimestamp = { year, month, _ ->
                    binding.tvStart.text = ("$year-$month")
                    val item = AttributeFieldItemFinder.findFirst(currentInstance,fieldCode = FIELD_START_DATE)
                    item?.let {
                        item.displayValue =  "$year-$month"
                        item.fieldValue = mapOf(
                            "value" to "$year-$month" ,
                            "displayText" to "$year-$month"
                        )
                    }
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
                onConfirmWithTimestamp = { year, month, _ ->
                    binding.tvEnd.text = ("$year-$month")
                    val item = AttributeFieldItemFinder.findFirst(currentInstance,fieldCode = FIELD_END_DATE)
                    item?.let {
                        item.displayValue =  "$year-$month"
                        item.fieldValue = mapOf(
                            "value" to "$year-$month" ,
                            "displayText" to "$year-$month"
                        )
                    }
                }
            )
        }
        // 是否全日制：单选（先做布尔编码转字符串）
        binding.vForm5.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance,fieldCode = FIELD_FULL_TIME)
            val list = Json.decodeFromString<List<CommonEnumItem>>(convertCodeBooleanToString(item?.questionOptionsJson?:"[]"))
            SingleSelectDialog.show(this.supportFragmentManager,"是否全日制",list){  data->
                binding.vForm5.setValueText(data.name)
                item?.let {
                    item.displayValue =  data.name
                    item.fieldValue = mapOf(
                        "value" to data.code ,
                        "displayText" to data.name
                    )
                }
            }
        }
        // 院校性质：单选
        binding.vForm6.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance,fieldCode = FIELD_SCHOOL_TYPE)
            val list = Json.decodeFromString<List<CommonEnumItem>>(item?.questionOptionsJson?:"[]")
            SingleSelectDialog.show(this.supportFragmentManager,"院校性质",list){  data->
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
        // GPA：自由填写
        binding.vForm7.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_GPA)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        // 排名 / 荣誉：自由填写
        binding.vForm8.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_RANKING_OR_HONORS)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        // 主修课程：自由填写
        binding.vForm9.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_MAIN_COURSES)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        // 描述：长文本填写
        binding.vForm10.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_DESCRIPTION)
            if (item != null) {
                PublicDescriptionActivity.start(this, isEdit, item,getString(R.string.educational_responsibilities))
            }
        }

        // 仅编辑模式显示删除按钮
        binding.viewActionButtons.setSecondaryVisible(isEdit)
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
        val schoolNameItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_SCHOOL_NAME)
        schoolNameItem?.let {
            binding.vForm1.setValueText(it.displayValue)
        }

        val degreeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_DEGREE)
        degreeItem?.let {
            binding.vForm2.setValueText(it.displayValue)
        }

        val majorItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_MAJOR)
        majorItem?.let {
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

        val fullTimeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_FULL_TIME)
        fullTimeItem?.let {
            binding.vForm5.setValueText(it.displayValue)
        }

        val schoolTypeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_SCHOOL_TYPE)
        schoolTypeItem?.let {
            binding.vForm6.setValueText(it.displayValue)
        }

        val gpaItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_GPA)
        gpaItem?.let {
            binding.vForm7.setValueText(it.displayValue)
        }

        val rankingItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_RANKING_OR_HONORS)
        rankingItem?.let {
            binding.vForm8.setValueText(it.displayValue)
        }

        val mainCoursesItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_MAIN_COURSES)
        mainCoursesItem?.let {
            binding.vForm9.setValueText(it.displayValue)
        }

        val descriptionItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_DESCRIPTION)
        descriptionItem?.let {
            binding.vForm10.setValueText(it.displayValue)
        }
    }

    private fun observeEvents() {
        // 监听子页面自由填写 / 长文本 / 单选回传事件
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
                        FIELD_SCHOOL_NAME -> {
                            binding.vForm1.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_MAJOR -> {
                            binding.vForm3.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_GPA -> {
                            binding.vForm7.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_RANKING_OR_HONORS -> {
                            binding.vForm8.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_MAIN_COURSES -> {
                            binding.vForm9.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_DESCRIPTION -> {
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
