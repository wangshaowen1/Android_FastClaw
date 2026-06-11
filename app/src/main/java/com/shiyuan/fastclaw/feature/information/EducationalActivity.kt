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
            PublicFreeFillInActivity.start(this, isEdit, CommonItems(fieldName = "院校名称"))
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
            PublicFreeFillInActivity.start(this, isEdit, CommonItems(fieldName = "GPA成绩"))
        }
        binding.vForm8.setOnSingleClickListener {
            PublicFreeFillInActivity.start(this, isEdit, CommonItems(fieldName = "在校排名/荣誉"))
        }
        binding.vForm9.setOnSingleClickListener {
            PublicFreeFillInActivity.start(this, isEdit, CommonItems(fieldName = "主修课程"))
        }
        binding.vForm10.setOnSingleClickListener {
            PublicDescriptionActivity.start(this, isEdit, CommonItems(fieldName = "教育补充描述"))
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
        schoolNameItem?.displayValue?.let {
            binding.vForm1.setData(CommonItems(fieldName = "院校名称", fieldValue = it))
        }

        val degreeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_DEGREE)
        degreeItem?.displayValue?.let {
            binding.vForm2.setData(CommonItems(fieldName = "学历", fieldValue = it))
        }

        val majorItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_MAJOR)
        majorItem?.displayValue?.let {
            binding.vForm3.setData(CommonItems(fieldName = "专业", fieldValue = it))
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
        fullTimeItem?.displayValue?.let {
            binding.vForm5.setData(CommonItems(fieldName = "是否全日制", fieldValue = it))
        }

        val schoolTypeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_SCHOOL_TYPE)
        schoolTypeItem?.displayValue?.let {
            binding.vForm6.setData(CommonItems(fieldName = "院校性质", fieldValue = it))
        }

        val gpaItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_GPA)
        gpaItem?.displayValue?.let {
            binding.vForm7.setData(CommonItems(fieldName = "GPA成绩", fieldValue = it))
        }

        val rankingItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_RANKING_OR_HONORS)
        rankingItem?.displayValue?.let {
            binding.vForm8.setData(CommonItems(fieldName = "在校排名/荣誉", fieldValue = it))
        }

        val mainCoursesItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_MAIN_COURSES)
        mainCoursesItem?.displayValue?.let {
            binding.vForm9.setData(CommonItems(fieldName = "主修课程", fieldValue = it))
        }

        val descriptionItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_DESCRIPTION)
        descriptionItem?.displayValue?.let {
            binding.vForm10.setData(CommonItems(fieldName = "教育补充描述", fieldValue = it))
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
