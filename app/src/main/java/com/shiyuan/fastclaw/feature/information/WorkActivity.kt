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
import com.shiyuan.fastclaw.databinding.ActivityWorkBinding
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
            PublicFreeFillInActivity.start(this, isEdit, CommonItems(fieldName = "公司名称"))
        }

        binding.vForm2.setOnSingleClickListener {

        }
        binding.tvStart.setOnClickListener {

        }
        binding.tvEnd.setOnClickListener {

        }
        binding.vForm3.setOnSingleClickListener {

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
        companyNameItem?.displayValue?.let {
            binding.vForm1.setData(CommonItems(fieldName = "公司名称", fieldValue = it))
        }

        val positionItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_POSITION)
        positionItem?.displayValue?.let {
            binding.vForm2.setData(CommonItems(fieldName = "职位名称", fieldValue = it))
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
        workContentItem?.displayValue?.let {
            binding.vForm3.setData(CommonItems(fieldName = "工作内容", fieldValue = it))
        }

        val industryItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_INDUSTRY)
        industryItem?.displayValue?.let {
            binding.vForm5.setData(CommonItems(fieldName = "所属行业", fieldValue = it))
        }

        val companyScaleItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COMPANY_SCALE)
        companyScaleItem?.displayValue?.let {
            binding.vForm6.setData(CommonItems(fieldName = "公司规模", fieldValue = it))
        }

        val companyTypeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_COMPANY_TYPE)
        companyTypeItem?.displayValue?.let {
            binding.vForm7.setData(CommonItems(fieldName = "公司类型", fieldValue = it))
        }

        val teamSizeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_TEAM_SIZE)
        teamSizeItem?.displayValue?.let {
            binding.vForm8.setData(CommonItems(fieldName = "团队规模", fieldValue = it))
        }

        val leavingReasonItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_LEAVING_REASON)
        leavingReasonItem?.displayValue?.let {
            binding.vForm9.setData(CommonItems(fieldName = "离职原因", fieldValue = it))
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
