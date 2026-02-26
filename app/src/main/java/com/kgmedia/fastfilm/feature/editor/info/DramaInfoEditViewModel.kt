package com.kgmedia.fastfilm.feature.editor.info

import android.graphics.Color
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.ext.showConfirmPrompt
import com.kgmedia.fastfilm.common.ext.topActivity
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.ext.requestWithLoading
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.network.apis.request.SSEContentBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkInfoUpdateBody
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.PlainTextSseProcessor
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger

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

    //编辑修改成功
    val editAndModify = MutableLiveData<Boolean>(false)

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
        requestWithLoading({
            mRepo.api.updateWorkInfo(
                WorkInfoUpdateBody(
                    workInfo?.workId,
                    cover.get(),
                    name.value,
                    desc.value
                )
            )
        }) {
            editAndModify.value = true
        }
    }

    private var sseHandler: SSEStreamHandler? = null

    fun sseGenerate() {
        val sseFlow = mRepo.api.sseWorkDescOptimize(
            SSEContentBody(
                desc.value,
                DramaGenerateConstants.MODEL_OPTIMIZE_DRAMA_DESC
            )
        )
        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            processor = PlainTextSseProcessor(
                scope = viewModelScope,
                onStart = {
                    Logger.d("[SSE]", "Open")
                    desc.value = ""
                },
                onUpdate = { fullContent ->
                    // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                    desc.value = fullContent
                },
                onComplete = {
                    Logger.d("[SSE]", "Finished")
                    // 触发原本结束后的逻辑
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    // 根据需要处理特定错误
                    parseMessage(e)?.showToast()
                })
        )
        // 3. 启动
        Logger.d("[SSE]", "Starting with content body = $desc")
        sseHandler?.start(sseFlow)
    }

    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        sseHandler?.cancel()
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