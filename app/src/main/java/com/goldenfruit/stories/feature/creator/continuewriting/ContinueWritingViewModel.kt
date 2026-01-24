package com.kgmedia.fastfilm.feature.creator.continuewriting

import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.User
import com.kgmedia.fastfilm.core.db.entity.SSEMessage
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.ext.parseMessage
import com.kgmedia.fastfilm.core.network.SSEvent
import com.kgmedia.fastfilm.core.network.apis.request.ContinueGenerateBody
import com.kgmedia.fastfilm.core.network.apis.request.DramasSeriesIdBody
import com.kgmedia.fastfilm.core.network.apis.response.SeriesWorkData
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class ContinueWritingViewModel : AppRepositoryViewModel<CreationRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = "未命名标题",
        titleColor = R.color.textColorPrimary.getResColor(),
        background = Color.TRANSPARENT,
    )

    val dramasSeriesId by lazy { mBundle.getString(Constants.Extra.ID) }
    val dramasSeriesName by lazy { mBundle.getString(Constants.Extra.NAME) }
    val input by lazy { MutableLiveData(Global.continueWritingContent[dramasSeriesId]) }
    val info = MutableLiveData<SeriesWorkData>()
    val textLimitNum by lazy { Config.get().appConfig.shortDramas }
    val minInputLength = 10
    val maxInputLength by lazy {
        ObservableInt(1000)
    }
    val promptGenerating = MutableLiveData<Pair<Boolean, String>>()

    val isVip = ObservableBoolean(User.get().isVip)

    private var sseChatJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        dramasSeriesName?.let {
            mToolbarVM.mTitle.set("续写：$it")
        }
        getDramaWorkInfo()
    }

    override fun onResume() {
        super.onResume()
        refreshConfig()
    }

    fun refreshConfig() {
        isVip.set(User.get().isVip)
        val maxLength = if (isVip.get()) textLimitNum?.shortDramasWriteStoryLimitNumForVip else textLimitNum?.shortDramasWriteStoryLimitNum
        maxInputLength.set(maxLength ?: (if (isVip.get()) 5000 else 1000))
    }

    fun getDramaWorkInfo(function: (() -> Unit)? = null) {
        launchWithLoading {
            response(mRepo.api.latestDramasWorkDetail(DramasSeriesIdBody(dramasSeriesId))) {
                info.value = data
                function?.invoke()
            }
        }
    }

    /**
     * 启动SSE内容生成过程
     */
    fun sseContinueGenerate() {
        promptGenerating.value = true to "正在续写，请稍候..."
        sseChatJob = launch {
            try {
                isSSEStreaming.set(true)
                val sseFlow = mRepo.api.aiContinueGenerate(ContinueGenerateBody(dramasSeriesId, input.value))
                sseFlow.collect { event ->
                    handleSSEvent(event)
                }
            } catch (e: CancellationException) {
                Logger.d("[SSE]", "主动取消协程")
                throw e // 重新抛出以确保取消流程完整
            } catch (e: Exception) {
                Log.e("[SSE]", "sse异常断开: e = ${e.fillInStackTrace()}")
                e.fillInStackTrace()
            } finally {
                promptGenerating.value = false to ""
                releaseJob()
            }
        }
    }


    /**
     * 处理SSE事件
     * @param event SSE事件对象，包括Open、Message、Error、Close等类型
     */
    private fun handleSSEvent(event: SSEvent) {
        when (event) {
            is SSEvent.Open -> {
                Logger.d("[SSE]", "Open")
                promptGenerating.value = false to ""
                sseBuffer = StringBuilder()
                input.value = ""
            }

            is SSEvent.Message -> {
                val data = event.data
                try {
                    val sseMessage = Json.decodeFromString<SSEMessage>(data)
                    if (sseMessage.code == SSEMessage.TYPE_ERROR) {
                        sseMessage.content?.showToast()
                        sseError()
                    } else {
                        Logger.d("[SSE]", "receive msg: parsing content message $sseMessage")
                        handlerSSEMessage(sseMessage)
                    }
                } catch (e: Exception) {
                    Log.e("[SSE]", "receive msg: parsing error for $data", e)
                    sseError()
                }
            }

            is SSEvent.Error -> {
                sseError()
                Log.e("[SSE]", "sse协议出错: ${event.throwable.fillInStackTrace()}")
            }

            is SSEvent.Close -> {
                Log.e("[SSE]", "sse协议 关闭")
                releaseJob()
            }
        }
    }

    private var sseBuffer: StringBuilder? = null
    val isSSEStreaming = ObservableBoolean(false)
    private var lastUiEmitTime = 0L

    /**
     * 处理SSE消息
     * @param sseMessage SSE消息对象
     */
    fun handlerSSEMessage(sseMessage: SSEMessage) {
        if (sseMessage.isOver()) {
            emitToUi(sseBuffer.toString(), true)
            sseBuffer?.clear()
            return
        }
        sseMessage.content?.let { content ->
            sseBuffer?.append(content)
            emitToUi(sseBuffer.toString())
        }
    }


    private fun emitToUi(content: String?, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastUiEmitTime < 50L) return
        lastUiEmitTime = now
        input.value = content
    }

    /**
     * SSE错误处理
     */
    private fun sseError() {
        isSSEStreaming.set(false)
        sseChatJob?.cancel()
        sseChatJob = null
    }

    /**
     * 释放SSE相关资源
     */
    private fun releaseJob() {
        isSSEStreaming.set(false)
        sseChatJob?.cancel()
        sseChatJob = null
    }
}