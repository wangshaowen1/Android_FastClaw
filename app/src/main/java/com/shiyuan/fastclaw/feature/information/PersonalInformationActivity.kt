package com.shiyuan.fastclaw.feature.information

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.nhsd.mattise.Matisse
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.load
import com.shiyuan.fastclaw.common.ext.selectSinglePhotoFromAlbums
import com.shiyuan.fastclaw.common.permissions.requestImages2VideosPermissions
import com.shiyuan.fastclaw.core.Events
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.network.apis.request.JobCityQueryBody
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.CommonEnumItem
import com.shiyuan.fastclaw.databinding.ActivityPersonalInformationBinding
import com.shiyuan.fastclaw.feature.information.dialog.BirthYearMonthPickerDialog
import com.shiyuan.fastclaw.feature.information.dialog.SingleSelectDialog
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel
import org.json.JSONObject

class PersonalInformationActivity :
    BaseViewModelActivity<ActivityPersonalInformationBinding, InformationViewModel>() {

    companion object {
        private const val REQUEST_CODE_AVATAR = 0x120

        private const val FIELD_AVATAR_URL = "avatarUrl"
        private const val FIELD_NAME = "name"
        private const val FIELD_GENDER = "gender"
        private const val FIELD_BIRTHDAY = "birthday"
        private const val DEFAULT_BIRTHDAY_YEAR = 2000
        private const val DEFAULT_BIRTHDAY_MONTH = 1
        private const val FIELD_PHONE = "phone"
        private const val FIELD_WECHAT_ID = "wechatId"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_LOCATION = "location"
        private const val FIELD_POLITICAL_STATUS = "politicalStatus"
        private const val FIELD_MARITAL_STATUS = "maritalStatus"
        private const val INPUT_MODE_SINGLE_CHOICE = "SINGLE_CHOICE"
        private const val INPUT_MODE_DICT_SINGLE = "DICT_SINGLE"
        private const val INPUT_MODE_DATE_MONTH = "DATE_MONTH"

        private const val FIELD_JOBIDENTITY = "jobIdentity"


        fun start(context: Context?) {
            context ?: return
            context.startActivity(Intent(context, PersonalInformationActivity::class.java))
        }
    }

    private var selectedAvatarUri: Uri? = null
    private val baseInfoItems = mutableListOf<AttributeArrayQueryData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObservers()
        mRealVM.queryOnlineResumeInfo()
    }

    private fun initObservers() {
        mRealVM.onlineResumeBaseInfo.observe(this) { baseInfo ->
            bindBaseInfo(baseInfo.orEmpty())
        }
        observeEvents {
            on<Events.CommonAttributeArrayQueryData> { event ->
                mRealVM.queryOnlineResumeInfo()
            }

            on<Events.ClawAgentEvent> { event ->
                when (event.type) {
                    AppEvent.KLX_C_EXPECTED_CURRENTLY_CITY_COMPLETED -> {
                        handleLocationCitySelected(event.msgGson)
                    }
                }

            }
        }
    }

    private fun handleLocationCitySelected(msgGson: String?) {
        val json = msgGson?.takeIf { it.isNotBlank() } ?: return
        val cityJson = runCatching { JSONObject(json) }.getOrNull() ?: return
        val code = cityJson.optString("code").takeIf { it.isNotBlank() } ?: return
        val name = cityJson.optString("name").takeIf { it.isNotBlank() } ?: return
        val item = findBaseInfoItem(FIELD_LOCATION)
        val value = mapOf(
            "code" to code,
            "name" to name
        )
        mRealVM.updateOnlineResumeAttribute(
            attributeGroupCode = item?.attributeGroupCode ?: item?.metaCode ?: FIELD_LOCATION,
            inputMode = item?.inputMode ?: INPUT_MODE_DICT_SINGLE,
            value = value
        ) {
            binding.currentCityRow.setValueText(name)
            FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
        }
    }

    private fun bindBaseInfo(baseInfo: List<AttributeArrayQueryData>) {
        baseInfoItems.clear()
        baseInfoItems.addAll(baseInfo)
        bindBaseInfo()
    }

    private fun initView() {
        mRealVM.mToolbarVM.mTitle.set(getString(R.string.personal_information_title))
        binding.ivAvatar.setOnSingleClickListener {
            requestAvatarPermission()
        }
        binding.nameRow.setOnSingleClickListener {
            PersonaFreeFillInActivity.start(this, isEdit = true, findBaseInfoItem(FIELD_NAME) ?: AttributeArrayQueryData(attributeGroupCode = FIELD_NAME, attributeGroupName = "姓名"))
        }
        binding.genderRow.setOnSingleClickListener {
            findBaseInfoItem(FIELD_GENDER)?.let { item ->
                val enumItems = item.options.orEmpty().map {
                    CommonEnumItem(code = it.code, name = it.name)
                }
                if (enumItems.isEmpty()) {
                    "暂无选项".showToast()
                    return@let
                }
                SingleSelectDialog.show(this.supportFragmentManager, "性别", enumItems) { data ->
                    val value = mapOf(
                        "code" to data.code,
                        "name" to data.name
                    )
                    mRealVM.updateOnlineResumeAttribute(
                        attributeGroupCode = item.attributeGroupCode ?: item.metaCode
                        ?: FIELD_GENDER,
                        inputMode = item.inputMode ?: INPUT_MODE_SINGLE_CHOICE,
                        value = value
                    ) {
                        binding.genderRow.setValueText(data.name.orEmpty())
                        FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
                    }
                }
            }
        }
        binding.birthDateRow.setOnSingleClickListener {
            val item = findBaseInfoItem(FIELD_BIRTHDAY)
            val selectedDate = getBaseInfoDate(FIELD_BIRTHDAY)
            BirthYearMonthPickerDialog.show(
                manager = supportFragmentManager,
                title = getString(R.string.birth_date),
                selectedYear = selectedDate?.first ?: DEFAULT_BIRTHDAY_YEAR,
                selectedMonth = selectedDate?.second ?: DEFAULT_BIRTHDAY_MONTH,
                maxTimestamp = System.currentTimeMillis(),
                onConfirmWithTimestamp = { year, month, _ ->
                    val displayText = "$year-$month"
                    mRealVM.updateOnlineResumeAttribute(
                        attributeGroupCode = item?.attributeGroupCode ?: item?.metaCode
                        ?: FIELD_BIRTHDAY,
                        inputMode = item?.inputMode ?: INPUT_MODE_DATE_MONTH,
                        value = displayText
                    ) {
                        binding.birthDateRow.setValueText(displayText)
                        FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
                    }
                }
            )
        }
        binding.phoneRow.setOnSingleClickListener {
            "手机号暂不能修改".showToast()
        }

        binding.JobStatusRow.setOnSingleClickListener {
            findBaseInfoItem(FIELD_JOBIDENTITY)?.let { item ->
                val enumItems = item.options.orEmpty().map {
                    CommonEnumItem(code = it.code, name = it.name)
                }
                if (enumItems.isEmpty()) {
                    "暂无选项".showToast()
                    return@let
                }
                SingleSelectDialog.show(this.supportFragmentManager, "求职身份", enumItems) { data ->
                    val value = mapOf(
                        "code" to data.code,
                        "name" to data.name
                    )
                    mRealVM.updateOnlineResumeAttribute(
                        attributeGroupCode = item.attributeGroupCode ?: item.metaCode
                        ?: FIELD_JOBIDENTITY,
                        inputMode = item.inputMode ?: INPUT_MODE_SINGLE_CHOICE,
                        value = value
                    ) {
                        binding.JobStatusRow.setValueText(data.name.orEmpty())
                        FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
                    }
                }
            }
        }
        binding.wechatRow.setOnSingleClickListener {
            PersonaFreeFillInActivity.start(
                this,
                isEdit = true,
                findBaseInfoItem(FIELD_WECHAT_ID) ?: AttributeArrayQueryData(
                    attributeGroupCode = FIELD_WECHAT_ID,
                    attributeGroupName = getString(R.string.personal_information_wechat)
                )
            )
        }
        binding.emailRow.setOnSingleClickListener {
            PersonaFreeFillInActivity.start(
                this,
                isEdit = true,
                findBaseInfoItem(FIELD_EMAIL) ?: AttributeArrayQueryData(
                    attributeGroupCode = FIELD_EMAIL,
                    attributeGroupName = getString(R.string.personal_information_email)
                )
            )
        }

        binding.currentCityRow.setOnSingleClickListener {
            JobCitySelectorActivity.start(this, JobCityQueryBody.LOCATION)
        }
        binding.politicalStatusRow.setOnSingleClickListener {
            findBaseInfoItem(FIELD_POLITICAL_STATUS)?.let { item ->
                val enumItems = item.options.orEmpty().map {
                    CommonEnumItem(code = it.code, name = it.name)
                }
                if (enumItems.isEmpty()) {
                    "暂无选项".showToast()
                    return@let
                }
                SingleSelectDialog.show(this.supportFragmentManager, "政治面貌", enumItems) { data ->
                    val value = mapOf(
                        "code" to data.code,
                        "name" to data.name
                    )
                    mRealVM.updateOnlineResumeAttribute(
                        attributeGroupCode = item.attributeGroupCode ?: item.metaCode
                        ?: FIELD_POLITICAL_STATUS,
                        inputMode = item.inputMode ?: INPUT_MODE_SINGLE_CHOICE,
                        value = value
                    ) {
                        binding.politicalStatusRow.setValueText(data.name.orEmpty())
                        FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
                    }
                }
            }
        }

        binding.maritalStatusRow.setOnSingleClickListener {
            findBaseInfoItem(FIELD_MARITAL_STATUS)?.let { item ->
                val enumItems = item.options.orEmpty().map {
                    CommonEnumItem(code = it.code, name = it.name)
                }
                if (enumItems.isEmpty()) {
                    "暂无选项".showToast()
                    return@let
                }
                SingleSelectDialog.show(this.supportFragmentManager, "婚姻状况", enumItems) { data ->
                    val value = mapOf(
                        "code" to data.code,
                        "name" to data.name
                    )
                    mRealVM.updateOnlineResumeAttribute(
                        attributeGroupCode = item.attributeGroupCode ?: item.metaCode
                        ?: FIELD_MARITAL_STATUS,
                        inputMode = item.inputMode ?: INPUT_MODE_SINGLE_CHOICE,
                        value = value
                    ) {
                        binding.maritalStatusRow.setValueText(data.name.orEmpty())
                        FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
                    }
                }
            }
        }
    }

    private fun bindBaseInfo() {
        val avatarUrl = findBaseInfoText(FIELD_AVATAR_URL)
        if (avatarUrl.isBlank()) {
            binding.ivAvatar.setImageResource(R.drawable.ic_home_header_avatar)
        } else {
            binding.ivAvatar.load(avatarUrl, R.drawable.ic_home_header_avatar)
        }
        binding.nameRow.setValueText(findBaseInfoText(FIELD_NAME))
        binding.genderRow.setValueText(findBaseInfoText(FIELD_GENDER))
        binding.birthDateRow.setValueText(findBaseInfoText(FIELD_BIRTHDAY))
        binding.phoneRow.setValueText(findBaseInfoText(FIELD_PHONE))
        binding.JobStatusRow.setValueText(findBaseInfoText(FIELD_JOBIDENTITY))
        binding.wechatRow.setValueText(findBaseInfoText(FIELD_WECHAT_ID))
        binding.emailRow.setValueText(findBaseInfoText(FIELD_EMAIL))
        binding.currentCityRow.setValueText(findBaseInfoText(FIELD_LOCATION))
        binding.politicalStatusRow.setValueText(findBaseInfoText(FIELD_POLITICAL_STATUS))
        binding.maritalStatusRow.setValueText(findBaseInfoText(FIELD_MARITAL_STATUS))
    }

    private fun findBaseInfoText(fieldCode: String): String {
        return findBaseInfoItem(fieldCode)?.baseDisplayText().orEmpty()
    }

    private fun findBaseInfoItem(fieldCode: String): AttributeArrayQueryData? {
        return baseInfoItems.firstOrNull {
            it.attributeGroupCode == fieldCode || it.metaCode == fieldCode
        }
    }

    private fun getBaseInfoDate(fieldCode: String): Pair<Int, Int>? {
        val parts = findBaseInfoText(fieldCode).split("-").takeIf { it.size == 2 } ?: return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
        return year to month
    }

    private fun AttributeArrayQueryData.baseDisplayText(): String {
        displayValue?.takeIf { it.isNotBlank() }?.let { return it }
        val rawValue = value ?: return ""
        if (rawValue is Map<*, *>) {
            rawValue["displayText"]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            rawValue["name"]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
            rawValue["value"]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return rawValue.toString()
    }


    private fun requestAvatarPermission() {
        requestImages2VideosPermissions(openSelect = true) { granted ->
            if (granted) {
                openAvatarPicker()
            }
        }
    }

    private fun openAvatarPicker() {
        selectSinglePhotoFromAlbums(
            activity = this,
            selectedUri = selectedAvatarUri,
            requestCode = REQUEST_CODE_AVATAR,
            capture = true,
            useEmbeddedCamera = true,
            crop = true
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_AVATAR && resultCode == RESULT_OK) {
            selectedAvatarUri = Matisse.obtainResult(data).firstOrNull()
            selectedAvatarUri?.let {
                //binding.ivAvatar.setImageURI(it)
                mRealVM.submitMultipleFiles(listOf(it)) { successUrls, _ ->
                    if (successUrls.isNotEmpty()) {
                        mRealVM.updateAvatar(successUrls.first()) { avatarUrl ->
                            binding.ivAvatar.load(avatarUrl, R.drawable.ic_home_header_avatar)
                            FlowEventBus.postSticky(Events.CommonAttributeArrayQueryData())
                        }
                    } else {
                        "头像上传失败".showToast()
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
