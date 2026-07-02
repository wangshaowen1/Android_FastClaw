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
import com.shiyuan.fastclaw.feature.information.dialog.DeleteConfirmDialog
import com.shiyuan.fastclaw.feature.information.dialog.SingleSelectDialog
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import kotlinx.serialization.json.Json

/**
 * 奖项项编辑页：支持新增或编辑一条获奖记录（奖项名称、奖励等级、获奖时间）。
 */
class AwardActivity : BaseViewModelActivity<ActivityAwardBinding, InformationViewModel>() {
    companion object {
        // Intent 参数 key
        private const val EXTRA_IS_EDIT = "extra_is_edit"
        private const val EXTRA_ATTRIBUTE_DATA = "extra_attribute_data"
        private const val EXTRA_INSTANCE_INDEX = "extra_instance_index"
        private const val CLIENTREQUESTID = "clientrequestid"
        private const val EXTRA_TYPE = "extra_type"

        // 字段编码
        private const val FIELD_AWARD_NAME = "awardName"
        private const val FIELD_AWARD_LEVEL = "awardLevel"
        private const val FIELD_AWARD_TIME = "awardTime"

        /**
         * 启动奖项编辑页。
         */
        fun start(
            activity: Activity?,
            isEdit: Boolean = true,
            attributeData: AttributeArrayQueryData? = null,
            position: Int? = null,
            clientRequestId: String? = null,
            type: String? = null
        ) {
            activity ?: return
            val intent = Intent(activity, AwardActivity::class.java).apply {
                putExtra(EXTRA_IS_EDIT, isEdit)
                attributeData?.let { putExtra(EXTRA_ATTRIBUTE_DATA, it) }
                position?.let { putExtra(EXTRA_INSTANCE_INDEX, it) }
                clientRequestId?.let { putExtra(CLIENTREQUESTID, it) }
                type?.let { putExtra(EXTRA_TYPE, it) }
            }
            activity.startActivity(intent)
        }
    }

    // 字段数据
    private var attributeData: AttributeArrayQueryData? = null

    // 当前编辑的字段实例索引
    private var position: Int = 0

    private var clientRequestId: String? = null

    // 当前编辑实例的字段项集合
    private val currentInstance: MutableList<AttributeFieldItem> = mutableListOf()

    // 是否编辑模式
    private var isEdit = true
    private var type = ""

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
        mRealVM.onResumeAttribute.observe(this) {
            FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
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
        clientRequestId = intent?.getStringExtra(CLIENTREQUESTID) ?: ""
        type = intent?.getStringExtra(EXTRA_TYPE) ?: ""
    }

    private fun initView() {
        if (isEdit) {
            // 编辑模式：显示删除/完成按钮
            binding.tvTitle.text = getString(R.string.edit_award)
            binding.viewActionButtons.setSecondaryText(getString(R.string.del))
            binding.viewActionButtons.setPrimaryText(getString(R.string.complete))
            // 删除当前实例并保存
            binding.viewActionButtons.setOnSecondaryClickListener {
                DeleteConfirmDialog.show(
                    manager = supportFragmentManager,
                    onDelete = {
                        attributeData?.let {
                            if (type == WorkActivity.TYPE_ONLINE_RESUME) {
                                mRealVM.onlineResumeAttributeDelete(it, position)
                            } else {
                                it.fieldTree?.removeAt(this.position)
                                it.draft = true
                                it.clientRequestId = this.clientRequestId
                                mRealVM.attributeSave(it)
                            }
                        }
                    }
                )
            }
            // 完成编辑：写回字段项并保存
            binding.viewActionButtons.setOnPrimaryClickListener {
                saveCurrentInstance()
            }
        } else {
            // 新增模式：仅显示保存按钮
            binding.tvTitle.text = getString(R.string.add_award)
            binding.viewActionButtons.setPrimaryText(getString(R.string.save))
            binding.viewActionButtons.setOnPrimaryClickListener {
                saveCurrentInstance()
            }
        }
        // 奖项名称：跳到自由填写页
        binding.vForm1.setOnSingleClickListener {
            val item = AttributeFieldItemFinder.findFirst(currentInstance, fieldCode = FIELD_AWARD_NAME)
            item?.let {
                PublicFreeFillInActivity.start(this, isEdit, it)
            }
        }

        // 奖励等级：弹出单选
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
        // 获奖时间：年月选择器
        binding.vForm3.setOnSingleClickListener {
            BirthYearMonthPickerDialog.show(
                manager = this.supportFragmentManager,
                title = "获奖时间",
                onConfirmWithTimestamp = { year, month, _ ->
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
            )
        }
        // 仅编辑模式显示删除按钮
        binding.viewActionButtons.setSecondaryVisible(isEdit)
    }

    private fun saveCurrentInstance() {
        attributeData?.fieldTree?.getOrNull(this.position)?.fieldItems = currentInstance
        attributeData?.draft = true
        attributeData?.clientRequestId = this.clientRequestId
        attributeData?.let {
            if (type == WorkActivity.TYPE_ONLINE_RESUME) {
                mRealVM.onResumeInfoAttributeSave(it, position, isEdit)
            } else {
                mRealVM.attributeSave(it)
            }
        }
    }

    /**
     * 把已有数据回填到表单控件。
     */
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
        // 监听自由填写 / 单选回传事件，按 fieldCode 同步到 currentInstance
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
        // 设置沉浸式状态栏
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .init()
    }

}
