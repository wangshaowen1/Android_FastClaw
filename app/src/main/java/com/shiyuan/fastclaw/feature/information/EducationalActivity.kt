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
import com.shiyuan.fastclaw.databinding.ActivityEducationalBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class EducationalActivity : BaseViewModelActivity<ActivityEducationalBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_ATTRIBUTE_DATA = "extra_attribute_data"
        private const val EXTRA_INSTANCE_INDEX = "extra_instance_index"

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

        fun start(activity: Activity?, isEdit: Boolean = true, attributeData: AttributeArrayQueryData? = null, instanceIndex: Long? = null) {
            activity ?: return
            val intent = Intent(activity, EducationalActivity::class.java).apply {
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
            binding.tvTitle.text = getString(R.string.edit_educational)
            binding.viewActionButtons.setSecondaryText(getString(R.string.del))
            binding.viewActionButtons.setPrimaryText(getString(R.string.complete))
            binding.viewActionButtons.setOnSecondaryClickListener {
                finish()
            }
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        } else {
            binding.tvTitle.text = getString(R.string.add_educational)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                finish()
            }
        }
        binding.vForm1.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_SCHOOL_NAME] ?: AttributeFieldItem(fieldCode = FIELD_SCHOOL_NAME, fieldName = "院校名称")
            } else {
                AttributeFieldItem(fieldCode = FIELD_SCHOOL_NAME, fieldName = "院校名称")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }

        binding.vForm2.setOnSingleClickListener {

        }
        binding.vForm3.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_MAJOR] ?: AttributeFieldItem(fieldCode = FIELD_MAJOR, fieldName = "专业")
            } else {
                AttributeFieldItem(fieldCode = FIELD_MAJOR, fieldName = "专业")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
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
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_GPA] ?: AttributeFieldItem(fieldCode = FIELD_GPA, fieldName = "GPA成绩")
            } else {
                AttributeFieldItem(fieldCode = FIELD_GPA, fieldName = "GPA成绩")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm8.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_RANKING_OR_HONORS] ?: AttributeFieldItem(fieldCode = FIELD_RANKING_OR_HONORS, fieldName = "在校排名/荣誉")
            } else {
                AttributeFieldItem(fieldCode = FIELD_RANKING_OR_HONORS, fieldName = "在校排名/荣誉")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm9.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_MAIN_COURSES] ?: AttributeFieldItem(fieldCode = FIELD_MAIN_COURSES, fieldName = "主修课程")
            } else {
                AttributeFieldItem(fieldCode = FIELD_MAIN_COURSES, fieldName = "主修课程")
            }
            PublicFreeFillInActivity.start(this, isEdit, fieldItem)
        }
        binding.vForm10.setOnSingleClickListener {
            val fieldItem = if (isEdit) {
                fieldItemsMap[FIELD_DESCRIPTION] ?: AttributeFieldItem(fieldCode = FIELD_DESCRIPTION, fieldName = "教育补充描述")
            } else {
                AttributeFieldItem(fieldCode = FIELD_DESCRIPTION, fieldName = "教育补充描述")
            }
            PublicDescriptionActivity.start(this, isEdit, fieldItem)
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

        val schoolNameItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_SCHOOL_NAME)
        schoolNameItem?.let {
            fieldItemsMap[FIELD_SCHOOL_NAME] = it
            binding.vForm1.setValueText(it.displayValue)
        }

        val degreeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_DEGREE)
        degreeItem?.let {
            fieldItemsMap[FIELD_DEGREE] = it
            binding.vForm2.setValueText(it.displayValue)
        }

        val majorItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_MAJOR)
        majorItem?.let {
            fieldItemsMap[FIELD_MAJOR] = it
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
            fieldItemsMap[FIELD_FULL_TIME] = it
            binding.vForm5.setValueText(it.displayValue)
        }

        val schoolTypeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_SCHOOL_TYPE)
        schoolTypeItem?.let {
            fieldItemsMap[FIELD_SCHOOL_TYPE] = it
            binding.vForm6.setValueText(it.displayValue)
        }

        val gpaItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_GPA)
        gpaItem?.let {
            fieldItemsMap[FIELD_GPA] = it
            binding.vForm7.setValueText(it.displayValue)
        }

        val rankingItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_RANKING_OR_HONORS)
        rankingItem?.let {
            fieldItemsMap[FIELD_RANKING_OR_HONORS] = it
            binding.vForm8.setValueText(it.displayValue)
        }

        val mainCoursesItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_MAIN_COURSES)
        mainCoursesItem?.let {
            fieldItemsMap[FIELD_MAIN_COURSES] = it
            binding.vForm9.setValueText(it.displayValue)
        }

        val descriptionItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_DESCRIPTION)
        descriptionItem?.let {
            fieldItemsMap[FIELD_DESCRIPTION] = it
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
                        FIELD_SCHOOL_NAME -> {
                            fieldItemsMap[FIELD_SCHOOL_NAME] = event.attributeFieldItem
                            binding.vForm1.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_MAJOR -> {
                            fieldItemsMap[FIELD_MAJOR] = event.attributeFieldItem
                            binding.vForm3.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_GPA -> {
                            fieldItemsMap[FIELD_GPA] = event.attributeFieldItem
                            binding.vForm7.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_RANKING_OR_HONORS -> {
                            fieldItemsMap[FIELD_RANKING_OR_HONORS] = event.attributeFieldItem
                            binding.vForm8.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_MAIN_COURSES -> {
                            fieldItemsMap[FIELD_MAIN_COURSES] = event.attributeFieldItem
                            binding.vForm9.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_DESCRIPTION -> {
                            fieldItemsMap[FIELD_DESCRIPTION] = event.attributeFieldItem
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
