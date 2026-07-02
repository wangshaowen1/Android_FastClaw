package com.shiyuan.fastclaw.feature.information

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.mattise.Matisse
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.load
import com.shiyuan.fastclaw.common.ext.selectSinglePhotoFromAlbums
import com.shiyuan.fastclaw.common.permissions.requestImages2VideosPermissions
import com.shiyuan.fastclaw.core.network.apis.response.AttributeArrayQueryData
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.databinding.ActivityPersonalInformationBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class PersonalInformationActivity :
    BaseViewModelActivity<ActivityPersonalInformationBinding, InformationViewModel>() {

    companion object {
        private const val REQUEST_CODE_AVATAR = 0x120
        private const val EXTRA_BASE_INFO = "extra_base_info"

        private const val FIELD_AVATAR_URL = "avatarUrl"
        private const val FIELD_NAME = "name"
        private const val FIELD_GENDER = "gender"
        private const val FIELD_PHONE = "phone"
        private const val FIELD_WECHAT_ID = "wechatId"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_LOCATION = "location"
        private const val FIELD_POLITICAL_STATUS = "politicalStatus"
        private const val FIELD_MARITAL_STATUS = "maritalStatus"

        fun start(context: Context?, baseInfo: List<AttributeArrayQueryData>? = null) {
            context ?: return
            val intent = Intent(context, PersonalInformationActivity::class.java).apply {
                putParcelableArrayListExtra(EXTRA_BASE_INFO, ArrayList(baseInfo.orEmpty()))
            }
            context.startActivity(intent)
        }
    }

    private var selectedAvatarUri: Uri? = null
    private val baseInfoItems = mutableListOf<AttributeFieldItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseIntent()
        initView()
    }

    private fun parseIntent() {
        val baseInfo: List<AttributeArrayQueryData> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(
                EXTRA_BASE_INFO,
                AttributeArrayQueryData::class.java
            ).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<AttributeArrayQueryData>(EXTRA_BASE_INFO).orEmpty()
        }
        baseInfoItems.clear()
        baseInfoItems.addAll(baseInfo.map { it.toFieldItem() })
    }

    private fun initView() {
        mRealVM.mToolbarVM.mTitle.set(getString(R.string.personal_information_title))
        bindBaseInfo()
        binding.nameRow.setOnSingleClickListener {
            PersonaFreeFillInActivity.start(
                this,
                isEdit = true,
                findBaseInfoItem(FIELD_NAME) ?: AttributeFieldItem(fieldCode = FIELD_NAME, fieldName = "姓名")
            )
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
        binding.phoneRow.setValueText(findBaseInfoText(FIELD_PHONE))
        binding.wechatRow.setValueText(findBaseInfoText(FIELD_WECHAT_ID))
        binding.emailRow.setValueText(findBaseInfoText(FIELD_EMAIL))
        binding.currentCityRow.setValueText(findBaseInfoText(FIELD_LOCATION))
        binding.politicalStatusRow.setValueText(findBaseInfoText(FIELD_POLITICAL_STATUS))
        binding.maritalStatusRow.setValueText(findBaseInfoText(FIELD_MARITAL_STATUS))
    }

    private fun findBaseInfoText(fieldCode: String): String {
        return findBaseInfoItem(fieldCode)?.displayValue.orEmpty()
    }

    private fun findBaseInfoItem(fieldCode: String): AttributeFieldItem? {
        return baseInfoItems.firstOrNull { it.fieldCode == fieldCode }
    }

    private fun AttributeArrayQueryData.toFieldItem(): AttributeFieldItem {
        return AttributeFieldItem(
            fieldCode = metaCode ?: attributeGroupCode,
            fieldName = attributeGroupName,
            fieldValue = value,
            displayValue = displayValue,
            inputMode = inputMode,
            dictType = dictType,
            dictApi = dictApi,
            options = options
        )
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.avatar_row -> requestAvatarPermission()
        }
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
            selectedAvatarUri?.let { binding.ivAvatar.setImageURI(it) }
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
