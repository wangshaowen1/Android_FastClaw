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
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.sensitiveWordCheck
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
import com.kgmedia.fastfilm.core.network.apis.request.ImgUrlBody
import com.kgmedia.fastfilm.core.network.apis.request.SSEContentBody
import com.kgmedia.fastfilm.core.network.apis.request.WorkInfoUpdateBody
import com.kgmedia.fastfilm.core.network.sse.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.sse.processor.PlainTextSseProcessor
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.DramaGenerateConstants
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val name = MutableLiveData<CharSequence>()
    val desc = MutableLiveData<CharSequence>()

    //编辑修改成功
    val editAndModify = MutableLiveData<Boolean>(false)

    //正在走流
    var isSSEGenerating = false

    override fun onCreate() {
        super.onCreate()
        workInfo?.apply {
            cover.set(workCoverUrl)
            name.value = workTitle ?: ""
            desc.value = workDesc ?: ""
        }
    }

    fun saveInfo() {
        val title = name.value?.toString()
        if (title.isNullOrEmpty()) {
            "请完善作品名称".showToast()
            return
        }
        val content = desc.value?.toString()
        if (content.isNullOrEmpty()) {
            "请完善作品简介".showToast()
            return
        }
        if (isUploading && !mCoverPath.isNullOrEmpty()) {
            "封面上传中,请稍等~".showToast()
            return
        }
        launchWithLoading {
            val sensitiveCheckRes =
                withContext(Dispatchers.IO) { mRepo.api.sensitiveImageCheck(ImgUrlBody(cover.get())) }
            val sensitiveCheckData = sensitiveCheckRes.data
            if (sensitiveCheckRes.isSuccess && sensitiveCheckData != null) {
                if (sensitiveCheckData.isSensitive()) {
                    sensitiveCheckData.content?.showToast()
                    return@launchWithLoading
                }
            } else {
                (sensitiveCheckRes.msg ?: "图片校验失败").showToast()
                return@launchWithLoading
            }

            sensitiveWordCheck(title).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { name.value = it }
                    return@launchWithLoading
                }
            }

            sensitiveWordCheck(content).let { (isPass, highlightText) ->
                if (!isPass) {
                    highlightText?.let { desc.value = it }
                    return@launchWithLoading
                }
            }
            response(
                mRepo.api.updateWorkInfo(
                    WorkInfoUpdateBody(
                        workInfo?.workId,
                        cover.get(),
                        title,
                        content
                    )
                )
            ) {
                editAndModify.value = true
            }
        }
    }

    private var sseHandler: SSEStreamHandler? = null

    fun sseGenerate() {
        if (isSSEGenerating) return
        isSSEGenerating = true
        setLoading(true)
        val sseFlow = mRepo.api.sseWorkDescOptimize(
            SSEContentBody(
                desc.value?.toString(),
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
                    setLoading(false)
                },
                onUpdate = { fullContent ->
                    // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                    desc.value = fullContent
                },
                onComplete = {
                    Logger.d("[SSE]", "Finished")
                    // 触发原本结束后的逻辑
                    isSSEGenerating = false
                    setLoading(false)
                },
                onErrorCallback = { e ->
                    Log.e("[SSE]", "Error: $e")
                    isSSEGenerating = false
                    // 根据需要处理特定错误
                    e?.message?.showToast()
                    setLoading(false)
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