package com.kgmedia.fastfilm.feature.settings.feedback

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.entity.KeyValueInfo
import com.kgmedia.fastfilm.core.network.apis.request.FeedbackBody
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2024/7/19 星期五 19:44
 * @Version: 1.0
 * *****************************************
 */

class FeedbackViewModel : AppRepositoryViewModel<AppRepository>() {
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "建议反馈"
    )

    val phoneNumber = MutableLiveData<String>()
    val photoSize = MutableLiveData<Int>(0)
    val type = ObservableField<KeyValueInfo?>()
    val typeNames by lazy { Config.get().appConfig.userFeedBackTypeList }
    override fun onCreate() {
        super.onCreate()
        photoSize.value = 0
    }

    fun feedback(content: String?, images: List<String>?) {
        setLoading(true)
        if (images.isNullOrEmpty()) {
            submit(content, null)
            return
        }
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(images, OssClient.TYPE_NORMAL, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val images = uploadSuccess.filterNotNull()
                if (images.isEmpty()) {
                    onUploadFail(null)
                } else {
                    submit(content, images)
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                "图片上传失败".showToast()
                setLoading(false)
            }
        })
    }

    private fun submit(content: String?, images: List<String>?) {
        launch({
            val body = FeedbackBody(type.get()?.key, content, images?.joinToString(separator = ","), phoneNumber.value)
            val response = withContext(Dispatchers.IO) { mRepo.api.feedback(body) }
            setLoading(false)
            if (response.isSuccess) {
                "反馈成功".showToast()
                finish()
            } else {
                response.msg.showToast()
            }
        }, {
            setLoading(false)
        })
    }
}