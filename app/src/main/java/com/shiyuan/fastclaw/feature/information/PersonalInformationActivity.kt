package com.shiyuan.fastclaw.feature.information

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.mattise.Matisse
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.selectSinglePhotoFromAlbums
import com.shiyuan.fastclaw.common.permissions.requestImages2VideosPermissions
import com.shiyuan.fastclaw.core.network.apis.response.AttributeFieldItem
import com.shiyuan.fastclaw.databinding.ActivityPersonalInformationBinding
import com.shiyuan.fastclaw.feature.information.viewModel.InformationViewModel

class PersonalInformationActivity :
    BaseViewModelActivity<ActivityPersonalInformationBinding, InformationViewModel>() {

    companion object {
        private const val REQUEST_CODE_AVATAR = 0x120

        fun start(context: Context?) {
            context ?: return
            context.startActivity(Intent(context, PersonalInformationActivity::class.java))
        }
    }

    private var selectedAvatarUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {
        mRealVM.mToolbarVM.mTitle.set(getString(R.string.personal_information_title))
        binding.nameRow.setOnSingleClickListener {
            PersonaFreeFillInActivity.start(this, isEdit = true,AttributeFieldItem(fieldName = "微信", displayValue ="微信"))
        }
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
