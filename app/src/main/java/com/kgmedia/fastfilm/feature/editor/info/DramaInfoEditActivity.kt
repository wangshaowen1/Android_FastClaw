package com.kgmedia.fastfilm.feature.editor.info

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.SimpleTextWatcher
import com.kgmedia.fastfilm.common.ext.selectSinglePhotoFromAlbums
import com.kgmedia.fastfilm.common.permissions.requestImages2VideosPermissions
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.databinding.ActivityWorkInfoEditBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.mattise.Matisse

/**
 * @Introduce:专辑-编辑详情，简介之类
 */
class DramaInfoEditActivity :
    BaseViewModelActivity<ActivityWorkInfoEditBinding, DramaInfoEditViewModel>() {

    companion object {
        private const val REQUEST_CODE_CHOOSE = 0x14

        @JvmStatic
        fun start(context: Context, workInfo: WorkInfo?) {
            val starter = Intent(context, DramaInfoEditActivity::class.java)
            starter.putExtra(Constants.Extra.DATA, workInfo)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.name.observe(this) {
            if (it != binding.nameInputer.text) {
                binding.nameInputer.setText(it)
            }
        }
        mRealVM.desc.observe(this) {
            if (it != binding.descInputer.text) {
                binding.descInputer.setText(it)
            }
        }
        binding.nameInputer.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != mRealVM.name.value) {
                    mRealVM.isModify = true
                }
                mRealVM.name.value = s.toString()
            }
        })
        binding.descInputer.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != mRealVM.desc.value) {
                    mRealVM.isModify = true
                }
                mRealVM.desc.value = s.toString()
            }
        })
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnChangeCover -> {
                requestAlbumsPermission()
            }

            R.id.btnConfirm -> {
                mRealVM.saveInfo()
            }
        }
    }

    private fun requestAlbumsPermission() {
        requestImages2VideosPermissions {
            if (it) {
                selectSinglePhotoFromAlbums(
                    activity = this@DramaInfoEditActivity,
                    selectedUri = null,
                    requestCode = REQUEST_CODE_CHOOSE,
                    crop = true
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_CHOOSE -> {
                    val result = Matisse.obtainPathResult(data)
                    result?.firstOrNull()?.let {
                        mRealVM.changeTargetPicture(it)
                    }
                }
            }
        }
    }
}