package com.kgmedia.fastfilm.feature.settings.feedback

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.permissions.requestImages2VideosPermissions
import com.kgmedia.fastfilm.common.utils.Utils
import com.kgmedia.fastfilm.databinding.ActivityFeedbackBinding
import com.kgmedia.fastfilm.widget.PhotoGridView
import com.kgmedia.fastfilm.widget.actionsheet.ActionSheetDialog
import com.kgmedia.fastfilm.widget.actionsheet.ActionSheetHelper
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.manager.FileManager
import com.nhsd.mattise.Matisse
import com.nhsd.mattise.MimeType
import com.nhsd.mattise.engine.impl.GlideEngine
import com.nhsd.mattise.internal.entity.CaptureStrategy


/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:投诉建议+举报
 * @Data: 2024/7/19 星期五 19:44
 * @Version: 1.0
 * *****************************************
 */
class FeedbackActivity :
    BaseViewModelActivity<ActivityFeedbackBinding, FeedbackViewModel>(), TextWatcher {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.photoGrid.setOnAddButtonClickedListener { requestAlbumsPermission() }
        binding.photoGrid.setOnPhotoChangedListener(object : PhotoGridView.OnPhotoChangedListener {
            override fun onPhotoAdded(photo: Uri?) {
                mRealVM.photoSize.value = binding.photoGrid.originalPhotos.size
            }

            override fun onPhotoRemoved(photo: Uri?) {
                mRealVM.photoSize.value = binding.photoGrid.originalPhotos.size
            }

        })
        binding.feedbackInput.addTextChangedListener(this)
        binding.contact.addTextChangedListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            val result = Matisse.obtainResult(data)
            binding.photoGrid.setAll(result as ArrayList<Uri?>, false)
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.commit -> {
                if (mRealVM.type.get() == null) {
                    "请选择类型".showToast()
                    return
                }
                if (binding.feedbackInput.text.length < 6) {
                    "请输入至少6个字的内容".showToast()
                    return
                }
                if (mRealVM.phoneNumber.value.isNullOrEmpty()) {
                    "请输入联系方式".showToast()
                    return
                }
                mRealVM.feedback(
                    binding.feedbackInput.text.toString(),
                    Utils.toPathList(binding.photoGrid.originalPhotos)
                )
            }

            R.id.typeLayout -> {
                ActionSheetHelper.createDialog(
                    mRealVM.typeNames?.mapNotNull { it.value }?.toTypedArray()
                ) { _: ActionSheetDialog?, position: Int ->
                    if (position >= 0) {
                        mRealVM.type.set(mRealVM.typeNames?.getOrNull(position))
                        updateButtonEnable()
                    }
                }.show(supportFragmentManager)
            }

        }
    }


    private fun requestAlbumsPermission() {
        requestImages2VideosPermissions {
            if (it) {
                //权限申请成功
                requestAlbums()
            } else {
                //权限申请失败
            }
        }
    }

    fun requestAlbums() {
        Matisse.from(this)
            .choose(MimeType.ofImage(), false)
            .countable(false)
            .capture(true)
            .captureStrategy(
                CaptureStrategy(true, FileManager.getInstance().fileProvider, "album")
            )
            .maxSelectable(4)
            .setSelectedUris(binding.photoGrid.copiedPhotos)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .thumbnailScale(0.85f)
            .imageEngine(GlideEngine())
            .showSingleMediaType(true)
            .originalEnable(Constants.ORIGINAL_ENABLE)
            .autoHideToolbarOnSingleTap(true)
            .forResult(REQUEST_CODE_CHOOSE)
    }


    companion object {
        const val REQUEST_CODE_CHOOSE: Int = 0x110

        fun start(
            activity: Activity?
        ) {
            activity ?: return
            val intent = Intent(activity, FeedbackActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        updateButtonEnable()
    }

    private fun updateButtonEnable() {
        binding.commit.alpha =
            if (mRealVM.phoneNumber.value?.isNotEmpty() == true && mRealVM.type.get() != null && (binding.feedbackInput.text?.length
                    ?: 0) >= 6
            ) {
                1f
            } else {
                0.5f
            }
    }
}

