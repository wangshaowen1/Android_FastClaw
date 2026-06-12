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
import com.shiyuan.fastclaw.databinding.ActivityAwardBinding
import com.shiyuan.fastclaw.feature.information.dialog.BirthYearMonthPickerDialog
import com.shiyuan.fastclaw.feature.information.dialog.SingleSelectDialog
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import kotlinx.serialization.json.Json

class AwardActivity : BaseViewModelActivity<ActivityAwardBinding, InformationViewModel>() {
    companion object {
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_ATTRIBUTE_DATA = "extra_attribute_data"
        private const val EXTRA_INSTANCE_INDEX = "extra_instance_index"

        private const val FIELD_AWARD_NAME = "awardName"
        private const val FIELD_AWARD_LEVEL = "awardLevel"
        private const val FIELD_AWARD_TIME = "awardTime"

        fun start(activity: Activity?, isEdit: Boolean = true, attributeData: AttributeArrayQueryData? = null, position: Int? = null) {
            activity ?: return
            val intent = Intent(activity, AwardActivity::class.java).apply {
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
            FlowEventBus.postSticky(Events.AttributeFieldSuccessfulEvent(attributeData))
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
            binding.tvTitle.text = getString(R.string.edit_award)
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
            binding.tvTitle.text = getString(R.string.add_award)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                attributeData?.fieldTree?.getOrNull(this.position)?.fieldItems = currentInstance
                attributeData?.draft = true
                attributeData?.let { mRealVM.attributeSave(it) }
            }
        }
        binding.vForm1.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_AWARD_NAME)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }

        binding.vForm2.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance,fieldCode = FIELD_AWARD_LEVEL)
            val list = Json.decodeFromString<List<CommonEnumItem>>(item?.questionOptionsJson?:"[]")
            SingleSelectDialog.show(this.supportFragmentManager,"奖励等级",list){  data->
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
        binding.vForm3.setOnSingleClickListener {
            BirthYearMonthPickerDialog.show(this.supportFragmentManager){ year,month->
                binding.vForm3.setValueText("$year-$month")
                val item = AttributeFieldItemFinder.findFirst(currentInstance,fieldCode = FIELD_AWARD_TIME)
                item?.let {
                    item.displayValue =  "$year-$month"
                    item.fieldValue = mapOf(
                        "value" to "$year-$month" ,
                        "displayText" to "$year-$month"
                    )
                }
            }
        }
        binding.viewActionButtons.setSecondaryVisible(isEdit)
    }

    private fun bindEditData() {
        val awardNameItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_AWARD_NAME)
        awardNameItem?.let {
            binding.vForm1.setValueText(it.displayValue)
        }

        val awardLevelItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_AWARD_LEVEL)
        awardLevelItem?.let {
            binding.vForm2.setValueText(it.displayValue)
        }

        val awardTimeItem = AttributeFieldItemFinder.findFirst(currentInstance, FIELD_AWARD_TIME)
        awardTimeItem?.let {
            binding.vForm3.setValueText(it.displayValue)
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
                        FIELD_AWARD_NAME -> {
                            binding.vForm1.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_AWARD_LEVEL -> {
                            binding.vForm2.setValueText(event.attributeFieldItem.displayValue)
                        }
                        FIELD_AWARD_TIME -> {
                            binding.vForm3.setValueText(event.attributeFieldItem.displayValue)
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
