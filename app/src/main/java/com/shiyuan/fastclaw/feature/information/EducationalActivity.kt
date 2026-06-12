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
import com.shiyuan.fastclaw.databinding.ActivityEducationalBinding
import com.shiyuan.fastclaw.feature.information.dialog.BirthYearMonthPickerDialog
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
            binding.tvTitle.text = getString(R.string.edit_educational)
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
            binding.tvTitle.text = getString(R.string.add_educational)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                attributeData?.fieldTree?.getOrNull(this.position)?.fieldItems = currentInstance
                attributeData?.draft = true
                attributeData?.let { mRealVM.attributeSave(it) }
            }
        }
        binding.vForm1.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_SCHOOL_NAME)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }

        binding.vForm2.setOnSingleClickListener {

        }
        binding.vForm3.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_MAJOR)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
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
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_GPA)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        binding.vForm8.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_RANKING_OR_HONORS)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        binding.vForm9.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_MAIN_COURSES)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }
        binding.vForm10.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_DESCRIPTION)
            if (item != null) {
                PublicDescriptionActivity.start(this, isEdit, item)
            }
        }

        binding.viewActionButtons.setSecondaryVisible(isEdit)
    }

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
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

}
