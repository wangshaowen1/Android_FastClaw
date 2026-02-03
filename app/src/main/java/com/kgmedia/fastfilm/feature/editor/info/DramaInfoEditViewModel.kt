package com.kgmedia.fastfilm.feature.editor.info

import android.graphics.Color
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.network.apis.request.WorkInfoUpdateBody
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast

class DramaInfoEditViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM by lazy {
        ToolbarViewModel(
            navigationAction = {
                if (isModify) {
                    topActivity?.showConfirmPrompt(
                        "您对作品的信息有修改尚未保存，是否保存修改?",
                        cancelText = "不保存",
                        confirmText = "保存并退出",
                        cancel = {
                            back()
                        },
                        confirm = {
                            saveInfo()
                        })
                    return@ToolbarViewModel
                }
                back()
            },
            title = "编辑作品",
            background = Color.TRANSPARENT
        )
    }

    val workInfo by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, WorkInfo::class.java) }

    private var mCoverPath: String? = null
    var isUploading = false
    var isModify = false

    val cover = ObservableField<String>()
    val name = MutableLiveData<String>()
    val desc = MutableLiveData<String>()

    override fun onCreate() {
        super.onCreate()
        workInfo?.apply {
            cover.set(workCoverUrl)
            name.value = workTitle ?: ""
            desc.value = workDesc ?: ""
        }
    }

    fun saveInfo() {
        if (name.value?.isEmpty() == true) {
            "请完善作品名称".showToast()
            return
        }
        if (name.value?.isEmpty() == true) {
            "请完善作品简介".showToast()
            return
        }
        if (isUploading && !mCoverPath.isNullOrEmpty()) {
            "封面上传中,请稍等~".showToast()
            return
        }
        requestWithLoading({ mRepo.api.updateWorkInfo(WorkInfoUpdateBody(workInfo?.workId, cover.get(), name.value, desc.value)) }) {
            finish()
        }
    }

    /**
     * @param path 本地路径
     */
    fun changeTargetPicture(path: String) {
        setLoading(true)
        mCoverPath = path
        isModify = true
        isUploading = true
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(path, OssClient.TYPE_PHOTO_IMG, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val url = uploadSuccess.firstOrNull()
                setLoading(false)
                if (url.isNullOrEmpty()) {
                    onUploadFail(null)
                } else {
                    cover.set(url)
                    "上传成功".showToast()
                }
                isUploading = false
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                setLoading(false)
                "上传失败".showToast()
                isUploading = false
            }
        })
    }
}