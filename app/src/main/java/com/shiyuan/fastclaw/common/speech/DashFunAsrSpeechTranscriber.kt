package com.shiyuan.fastclaw.common.speech

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.alibaba.idst.nui.AsrResult
import com.alibaba.idst.nui.Constants
import com.alibaba.idst.nui.INativeNuiCallback
import com.alibaba.idst.nui.KwsResult
import com.alibaba.idst.nui.NativeNui
import org.json.JSONObject

/**
 * 百炼 DashScope Fun-ASR 实时语音识别封装。
 *
 * 负责管理 NUI SDK 初始化、AudioRecord 录音、实时音频送入、识别结果解析和音量回调。
 * 页面侧只需要调用 [start]、[stop]、[cancel]、[release]，并通过 [Callback] 接收识别文本。
 */
class DashFunAsrSpeechTranscriber(
    private val context: Context,
    private val config: Config,
    private val callback: Callback,
) : INativeNuiCallback {

    /**
     * Fun-ASR 实时识别配置。
     *
     * [apiKey] 建议使用服务端下发的临时鉴权 token；[url] 默认使用 DashScope websocket 地址。
     */
    data class Config(
        val apiKey: String,
        val url: String = DEFAULT_URL,
        val model: String = DEFAULT_MODEL,
        val sampleRate: Int = SAMPLE_RATE,
        val format: String = FORMAT_PCM,
    ) {
        fun isValid(): Boolean = apiKey.isNotBlank()
    }

    /**
     * 识别过程回调，全部切回主线程触发，方便页面直接更新 UI 或发送消息。
     */
    interface Callback {
        fun onStarted() = Unit
        fun onPartialResult(text: String) = Unit
        fun onFinalResult(text: String) = Unit
        fun onVolumeChanged(rmsDb: Float) = Unit
        fun onError(message: String) = Unit
        fun onStopped() = Unit
    }

    companion object {
        private const val TAG = "DashFunAsrST"
        private const val SAMPLE_RATE = 16000
        private const val WAVE_FRAME_SIZE = 20 * 2 * 1 * SAMPLE_RATE / 1000
        private const val FORMAT_PCM = "pcm"
        private const val DEFAULT_MODEL = "fun-asr-realtime"
        private const val DEFAULT_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference"
    }

    private val nui = NativeNui()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val workerThread = HandlerThread("DashFunAsrSpeechTranscriber")
    private val workerHandler: Handler
    private var audioRecord: AudioRecord? = null
    private var initialized = false
    private var recognizing = false

    init {
        workerThread.start()
        workerHandler = Handler(workerThread.looper)
    }

    /**
     * 开始一次按住说话识别。
     *
     * 会按顺序检查配置、麦克风权限、SDK 初始化和录音器状态；返回 false 表示本次未成功启动。
     */
    fun start(): Boolean {
        if (recognizing) return true
        if (!config.isValid()) {
            notifyMain { callback.onError("请先配置百炼 DashScope API Key") }
            return false
        }
        if (!hasRecordPermission()) {
            notifyMain { callback.onError("请开启麦克风权限后再使用语音输入") }
            return false
        }
        if (!initialize()) return false
        if (!prepareAudioRecord()) return false

        workerHandler.post {
            // 参考 DashFunAsrSpeechTranscriberActivity：先设置识别参数，再传 apikey 启动会话。
            nui.setParams(buildRecognizerParams())
            val ret = nui.startDialog(Constants.VadMode.TYPE_P2T, buildDialogParams())
            if (ret == Constants.NuiResultCode.SUCCESS) {
                recognizing = true
                notifyMain { callback.onStarted() }
            } else {
                releaseAudioRecord()
                notifyMain { callback.onError("语音识别启动失败：$ret") }
            }
        }
        return true
    }

    /**
     * 正常结束当前识别会话。
     *
     * 用于“松开发送”：通知 SDK 停止会话，等待最终结果或完成事件回调。
     */
    fun stop() {
        if (!recognizing) {
            releaseAudioRecord()
            notifyMain { callback.onStopped() }
            return
        }
        workerHandler.post {
            nui.stopDialog()
            recognizing = false
        }
    }

    /**
     * 取消当前识别会话并释放录音资源。
     *
     * 用于“上滑取消”：丢弃本轮识别结果，不再等待最终文本。
     */
    fun cancel() {
        if (recognizing) {
            workerHandler.post {
                nui.stopDialog()
                recognizing = false
                releaseAudioRecord()
                notifyMain { callback.onStopped() }
            }
        } else {
            releaseAudioRecord()
            notifyMain { callback.onStopped() }
        }
    }

    /**
     * 释放 SDK、录音器和工作线程。
     *
     * 页面销毁时调用，避免录音资源和 HandlerThread 泄漏。
     */
    fun release() {
        cancel()
        workerHandler.post {
            nui.release()
            workerThread.quitSafely()
        }
    }

    /**
     * 初始化 NUI SDK。
     *
     * 初始化参数只放 websocket 地址、日志、重连等公共配置；apikey 在每次 startDialog 时传入。
     */
    private fun initialize(): Boolean {
        if (initialized) return true
        val ret = nui.initialize(
            this,
            buildInitParams(),
            Constants.LogLevel.LOG_LEVEL_INFO,
            true,
        )
        initialized = ret == Constants.NuiResultCode.SUCCESS
        if (!initialized) {
            notifyMain { callback.onError("语音识别初始化失败：$ret") }
        }
        return initialized
    }

    /**
     * 构建 SDK 初始化参数。
     */
    private fun buildInitParams(): String {
        val debugPath = context.externalCacheDir?.absolutePath.orEmpty()
        return JSONObject()
            // DashScope Fun-ASR 直连使用 websocket 地址，鉴权 apikey 在 startDialog 时单独传入。
            .put("device_id", "android_device")
            .put("url", config.url.ifBlank { DEFAULT_URL })
            .put("save_wav", "false")
            .put("debug_path", debugPath)
            .put("max_log_file_size", 50 * 1024 * 1024)
            .put("enable_reconnection", "true")
            .put("log_track_level", Constants.LogLevel.toInt(Constants.LogLevel.LOG_LEVEL_NONE).toString())
            .put("service_mode", Constants.ModeFullCloud)
            .toString()
    }

    /**
     * 构建实时识别参数。
     *
     * 参数结构与官方 DashFunAsrSpeechTranscriberActivity 示例一致。
     */
    private fun buildRecognizerParams(): String {
        val nlsConfig = JSONObject()
            // 与 DashFunAsrSpeechTranscriberActivity 保持一致：实时识别模型 + PCM 16K 单声道。
            .put("sr_format", config.format)
            .put("model", config.model)
            .put("sample_rate", config.sampleRate)
            .put("semantic_punctuation_enabled", true)
            .put("punctuation_prediction_enabled", true)

        return JSONObject()
            .put("nls_config", nlsConfig)
            .put("service_type", Constants.kServiceTypeSpeechTranscriber)
            .toString()
    }

    /**
     * 构建单次会话鉴权参数。
     */
    private fun buildDialogParams(): String {
        return JSONObject()
            .put("apikey", config.apiKey)
            .toString()
    }

    /**
     * 初始化系统录音器。
     *
     * Fun-ASR 示例使用 16bit、单声道、8K/16K 采样率；当前默认 16K PCM。
     */
    @SuppressLint("MissingPermission")
    private fun prepareAudioRecord(): Boolean {
        if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) return true
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            config.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            WAVE_FRAME_SIZE * 4,
        )
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            releaseAudioRecord()
            notifyMain { callback.onError("麦克风初始化失败") }
            return false
        }
        return true
    }

    /**
     * 释放 AudioRecord，允许下一次按住说话重新创建录音器。
     */
    private fun releaseAudioRecord() {
        runCatching { audioRecord?.release() }
        audioRecord = null
    }

    /**
     * 检查麦克风权限，权限请求由页面层负责。
     */
    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * NUI 事件回调。
     *
     * partial 更新临时文本，sentence end 作为可发送结果，complete/error 负责收尾。
     */
    override fun onNuiEventCallback(
        event: Constants.NuiEvent,
        resultCode: Int,
        arg2: Int,
        kwsResult: KwsResult?,
        asrResult: AsrResult?,
    ) {
        Log.d(TAG, "event=$event resultCode=$resultCode")
        when (event) {
            Constants.NuiEvent.EVENT_ASR_PARTIAL_RESULT -> asrResult.extractText()?.let { text ->
                notifyMain { callback.onPartialResult(text) }
            }
            Constants.NuiEvent.EVENT_SENTENCE_END -> asrResult.extractText()?.let { text ->
                notifyMain { callback.onFinalResult(text) }
            }
            Constants.NuiEvent.EVENT_TRANSCRIBER_COMPLETE -> {
                recognizing = false
                releaseAudioRecord()
                notifyMain { callback.onStopped() }
            }
            Constants.NuiEvent.EVENT_ASR_ERROR,
            Constants.NuiEvent.EVENT_MIC_ERROR -> {
                recognizing = false
                releaseAudioRecord()
                notifyMain {
                    callback.onError(asrResult?.asrResult?.takeIf { it.isNotBlank() } ?: "语音识别失败：$resultCode")
                    callback.onStopped()
                }
            }
            else -> Unit
        }
    }

    /**
     * SDK 拉取音频数据时回调，将麦克风 PCM 数据写入 SDK 提供的 buffer。
     */
    override fun onNuiNeedAudioData(buffer: ByteArray, len: Int): Int {
        val recorder = audioRecord ?: return -1
        if (recorder.state != AudioRecord.STATE_INITIALIZED) return -1
        return recorder.read(buffer, 0, len)
    }

    /**
     * SDK 音频状态回调，打开时启动录音，关闭或暂停时释放录音器。
     */
    override fun onNuiAudioStateChanged(state: Constants.AudioState) {
        when (state) {
            Constants.AudioState.STATE_OPEN -> runCatching { audioRecord?.startRecording() }
            Constants.AudioState.STATE_CLOSE,
            Constants.AudioState.STATE_PAUSE -> releaseAudioRecord()
            else -> Unit
        }
    }

    /**
     * SDK 音量回调，用于驱动录音浮层波形动画。
     */
    override fun onNuiAudioRMSChanged(value: Float) {
        notifyMain { callback.onVolumeChanged(value) }
    }

    override fun onNuiVprEventCallback(event: Constants.NuiVprEvent?) = Unit

    override fun onNuiLogTrackCallback(level: Constants.LogLevel?, log: String?) {
        Log.d(TAG, "level=$level log=$log")
    }

    /**
     * 从 SDK 结果对象中提取识别文本。
     *
     * asrResult 可能是纯文本，也可能是完整 JSON；JSON 场景继续走 [extractSentenceText]。
     */
    private fun AsrResult?.extractText(): String? {
        val direct = this?.asrResult?.trim().orEmpty()
        // Fun-ASR 有时会把完整 JSON 放到 asrResult；只有纯文本才直接返回。
        if (direct.isNotEmpty() && direct != "{}" && !direct.startsWith("{")) return direct
        return extractSentenceText(direct) ?: extractSentenceText(this?.allResponse.orEmpty())
    }

    /**
     * 从 DashScope Fun-ASR JSON 响应中解析 sentence.text。
     */
    private fun extractSentenceText(response: String): String? {
        if (response.isBlank()) return null
        return runCatching {
            // DashScope 返回文本位于 payload.output.sentence.text。
            JSONObject(response)
                .optJSONObject("payload")
                ?.optJSONObject("output")
                ?.optJSONObject("sentence")
                ?.optString("text")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    /**
     * 将 SDK 工作线程回调切回主线程。
     */
    private fun notifyMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
