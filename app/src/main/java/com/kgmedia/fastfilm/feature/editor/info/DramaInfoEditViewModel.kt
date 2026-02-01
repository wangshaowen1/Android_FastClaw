package com.kgmedia.fastfilm.feature.editor.info

import android.graphics.Color
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.request.WorkInfoUpdateBody
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.showToast

class DramaInfoEditViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM by lazy {
        ToolbarViewModel(
            
            navigationAction = { back() },
            title = "编辑专辑信息",
            menuText = "保存",
            background = Color.TRANSPARENT,
            menuAction = {
//                saveAlbumInfo()
            }
        )
    }
//    val mAlbumInfo: AlbumInfo? by lazy {
//        mBundle.getParcelable(Constants.Extra.DATA)
//    }

    private var mCoverPath: String? = null
    var isUploading = false

    val cover = ObservableField<String>()
    val name = MutableLiveData<String>()
    val desc = MutableLiveData<String>()

    override fun onCreate() {
        super.onCreate()
//        mAlbumInfo?.apply {
//            cover.set(albumCoverUrl)
//            name.value = albumName ?: ""
//            desc.value = albumRemark ?: ""
//        }
    }

    private fun saveInfo() {
        if (name.value?.isEmpty() == true) {
            "请完善作品名称".showToast()
            return
        }
        if (isUploading && !mCoverPath.isNullOrEmpty()) {
            "封面上传中,请稍等~".showToast()
            return
        }
        requestWithLoading({mRepo.api.updateWorkInfo(WorkInfoUpdateBody( "",cover.get(), name.value, desc.value))}) {
//                mAlbumInfo?.albumRemark = desc.value
//                mAlbumInfo?.albumName = name.value
//                mAlbumInfo?.albumCoverUrl = cover.get()
//                Global.albumInfoMap[mAlbumInfo?.albumId] = mAlbumInfo
//                FlowEventBus.postSync(AlbumChangeEvent(mAlbumInfo?.albumId, AlbumChangeEvent.EVENT_INFO_CHANGE, mAlbumInfo))
                finish()
        }
    }

    /**
     * @param path 本地路径
     */
    fun changeTargetPicture(path: String) {
        setLoading(true)
        mCoverPath = path
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