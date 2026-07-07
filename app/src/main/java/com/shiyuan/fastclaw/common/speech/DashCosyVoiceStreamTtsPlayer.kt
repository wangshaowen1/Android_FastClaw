package com.shiyuan.fastclaw.common.speech

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.alibaba.idst.nui.Constants
import com.alibaba.idst.nui.INativeStreamInputTtsCallback
import com.alibaba.idst.nui.NativeNui
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 百炼 CosyVoice 流式语音合成封装。
 *
 * 负责管理 NUI SDK 的流式 TTS 会话、音频数据回调和 PCM 播放。页面侧只需要调用
 * [speak]、[stop]、[release]，不用直接接触 NativeNui 和 AudioTrack。
 */
class DashCosyVoiceStreamTtsPlayer(
    private val context: Context,
    private val configProvider: () -> Config,
    private val callback: Callback = Callback.EMPTY,
) {

    /**
     * CosyVoice TTS 配置。
     *
     * [apiKey] 建议使用服务端下发的临时鉴权 token；联调阶段为空时由页面决定是否回退到本地配置。
     */
    data class Config(
        val apiKey: String,
        val url: String = DEFAULT_URL,
        val model: String = DEFAULT_MODEL,
        val voice: String = DEFAULT_VOICE,
        val format: String = FORMAT_PCM,
        val sampleRate: Int = SAMPLE_RATE,
        val volume: Int = DEFAULT_VOLUME,
        val rate: Float = DEFAULT_RATE,
        val pitch: Float = DEFAULT_PITCH,
    ) {
        fun isValid(): Boolean = apiKey.isNotBlank()
    }

    /**
     * 播放状态回调，全部切回主线程触发。
     */
    interface Callback {
        fun onStarted() = Unit
        fun onCompleted() = Unit
        fun onError(message: String) = Unit

        companion object {
            val EMPTY = object : Callback {}
        }
    }

    companion object {
        private const val TAG = "DashCosyVoiceTts"
        private const val DEFAULT_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference"
        private const val DEFAULT_MODEL = "cosyvoice-v3-flash"
        private const val DEFAULT_VOICE = "longyumi_v3"
        private const val FORMAT_PCM = "pcm"
        private const val SAMPLE_RATE = 24000
        private const val DEFAULT_VOLUME = 50
        private const val DEFAULT_RATE = 1.0f
        private const val DEFAULT_PITCH = 1.0f
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val workerThread = HandlerThread("DashCosyVoiceStreamTtsPlayer")
    private val workerHandler: Handler
    private val nui = NativeNui(Constants.ModeType.MODE_STREAM_INPUT_TTS)
    private var audioPlayer: PcmStreamPlayer? = null
    @Volatile private var speaking = false
    @Volatile private var streamInputOpened = false

    init {
        workerThread.start()
        workerHandler = Handler(workerThread.looper)
    }

    /**
     * 播报一段文本。
     *
     * 如果上一段还在合成或播放，会先取消上一段，保证面试官问题不会叠音。
     */
    fun speak(text: String): Boolean {
        val speakText = text.trim()
        if (speakText.isEmpty()) return false
        val config = configProvider()
        if (!config.isValid()) {
            notifyMain { callback.onError("请先配置百炼 CosyVoice API Key") }
            return false
        }
        workerHandler.post {
            stopInternal(releasePlayer = true)
            startStreamTts(speakText, config)
        }
        return true
    }

    /**
     * 启动一段可持续追加文本的流式 TTS 会话。
     */
    fun startStreaming(): Boolean {
        val config = configProvider()
        if (!config.isValid()) {
            notifyMain { callback.onError("请先配置百炼 CosyVoice API Key") }
            return false
        }
        workerHandler.post {
            stopInternal(releasePlayer = true)
            startStreamTtsSession(config)
        }
        return true
    }

    /**
     * 向当前流式 TTS 会话追加增量文本。
     */
    fun appendStreamingText(text: String) {
        val speakText = text.trim()
        if (speakText.isEmpty()) return
        workerHandler.post {
            if (!speaking || !streamInputOpened) return@post
            splitSentences(speakText).forEach { sentence ->
                if (!speaking) return@forEach
                nui.sendStreamInputTts(sentence)
            }
        }
    }

    /**
     * 结束当前流式 TTS 文本输入，等待已生成音频播放完成。
     */
    fun finishStreaming() {
        workerHandler.post {
            if (!streamInputOpened) return@post
            streamInputOpened = false
            nui.asyncStopStreamInputTts()
        }
    }

    /**
     * 停止当前播报。
     */
    fun stop() {
        workerHandler.post { stopInternal(releasePlayer = true) }
    }

    /**
     * 同步停止当前播报并清空播放器缓冲。
     *
     * 开始录音前使用，避免异步 stop 尚未执行时 ASR 已经打开，录入 TTS 尾音。
     */
    fun stopBlocking(timeoutMillis: Long = 500L) {
        if (Looper.myLooper() == workerHandler.looper) {
            stopInternal(releasePlayer = true)
            return
        }
        val latch = CountDownLatch(1)
        workerHandler.post {
            stopInternal(releasePlayer = true)
            latch.countDown()
        }
        runCatching { latch.await(timeoutMillis, TimeUnit.MILLISECONDS) }
    }

    /**
     * 释放 TTS SDK 和播放器资源。
     */
    fun release() {
        workerHandler.post {
            stopInternal(releasePlayer = true)
            runCatching { nui.release() }
            workerThread.quitSafely()
        }
    }

    private fun startStreamTts(text: String, config: Config) {
        if (!startStreamTtsSession(config)) return
        splitSentences(text).forEach { sentence ->
            if (!speaking) return@forEach
            nui.sendStreamInputTts(sentence)
        }
        finishStreaming()
    }

    private fun startStreamTtsSession(config: Config): Boolean {
        val taskId = UUID.randomUUID().toString()
        audioPlayer = PcmStreamPlayer(config.sampleRate, object : PcmStreamPlayer.Callback {
            override fun onPlayOver() {
                notifyMain { callback.onCompleted() }
            }
        })
        val ret = nui.startStreamInputTts(
            buildTtsCallback(config),
            buildTicket(config),
            buildParameters(config),
            taskId,
            Constants.LogLevel.toInt(Constants.LogLevel.LOG_LEVEL_INFO),
            true,
        )
        if (ret != Constants.NuiResultCode.SUCCESS) {
            speaking = false
            streamInputOpened = false
            audioPlayer?.release()
            audioPlayer = null
            notifyMain { callback.onError("语音播报启动失败：$ret") }
            return false
        }
        speaking = true
        streamInputOpened = true
        return true
    }

    private fun stopInternal(releasePlayer: Boolean) {
        speaking = false
        streamInputOpened = false
        runCatching { nui.cancelStreamInputTts() }
        audioPlayer?.stop()
        if (releasePlayer) {
            audioPlayer?.release()
            audioPlayer = null
        }
    }

    private fun buildTtsCallback(config: Config): INativeStreamInputTtsCallback {
        return object : INativeStreamInputTtsCallback {
            override fun onStreamInputTtsEventCallback(
                event: INativeStreamInputTtsCallback.StreamInputTtsEvent,
                taskId: String?,
                sessionId: String?,
                retCode: Int,
                errorMsg: String?,
                timestamp: String?,
                allResponse: String?,
            ) {
                Log.d(TAG, "event=$event taskId=$taskId retCode=$retCode")
                when (event) {
                    INativeStreamInputTtsCallback.StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_SYNTHESIS_STARTED -> {
                        audioPlayer?.setSampleRate(config.sampleRate)
                        audioPlayer?.play()
                        notifyMain { callback.onStarted() }
                    }
                    INativeStreamInputTtsCallback.StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_SYNTHESIS_COMPLETE -> {
                        speaking = false
                        streamInputOpened = false
                        audioPlayer?.finishSend()
                    }
                    INativeStreamInputTtsCallback.StreamInputTtsEvent.STREAM_INPUT_TTS_EVENT_TASK_FAILED -> {
                        speaking = false
                        streamInputOpened = false
                        audioPlayer?.stop()
                        Log.e(TAG, "tts task failed retCode=$retCode errorMsg=$errorMsg response=$allResponse")
                        notifyMain { callback.onError(errorMsg?.takeIf { it.isNotBlank() } ?: "语音播报失败：$retCode") }
                    }
                    else -> Unit
                }
            }

            override fun onStreamInputTtsDataCallback(data: ByteArray?) {
                data?.takeIf { it.isNotEmpty() }?.let { audioPlayer?.setAudioData(it) }
            }

            override fun onStreamInputTtsLogTrackCallback(level: Constants.LogLevel?, log: String?) {
                Log.d(TAG, "level=$level log=$log")
            }
        }
    }

    private fun buildTicket(config: Config): String {
        val debugPath = context.externalCacheDir?.absolutePath.orEmpty()
        return JSONObject()
            .put("apikey", config.apiKey)
            .put("device_id", "android_device")
            .put("url", config.url.ifBlank { DEFAULT_URL })
            .put("debug_path", debugPath)
            .put("max_log_file_size", 50 * 1024 * 1024)
            .put("log_track_level", Constants.LogLevel.toInt(Constants.LogLevel.LOG_LEVEL_NONE).toString())
            .toString()
    }

    private fun buildParameters(config: Config): String {
        return JSONObject()
            .put("model", config.model)
            .put("voice", config.voice)
            // 当前播放器直接播放 PCM；如果后续改 mp3/opus，需要打开 SDK 解码并确认输出仍为 PCM。
            .put("format", config.format)
            .put("enable_audio_decoder", false)
            .put("sample_rate", config.sampleRate)
            .put("volume", config.volume)
            .put("rate", config.rate)
            .put("pitch", config.pitch)
            .toString()
    }

    private fun splitSentences(text: String): List<String> {
        val result = mutableListOf<String>()
        val builder = StringBuilder()
        text.forEach { char ->
            builder.append(char)
            if (char in listOf('。', '！', '？', '!', '?', ';', '；', '\n')) {
                builder.toString().trim().takeIf { it.isNotEmpty() }?.let(result::add)
                builder.clear()
            }
        }
        builder.toString().trim().takeIf { it.isNotEmpty() }?.let(result::add)
        return result
    }

    private fun notifyMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    /**
     * PCM 播放队列，消费 CosyVoice 回调的音频流。
     */
    private class PcmStreamPlayer(
        private var sampleRate: Int,
        private val callback: Callback,
    ) {
        interface Callback {
            fun onPlayOver() = Unit
        }

        private enum class PlayState {
            IDLE, PLAYING, RELEASED
        }

        private val audioQueue = LinkedBlockingQueue<ByteArray>()
        @Volatile private var playState = PlayState.IDLE
        @Volatile private var finishSend = false
        private var audioTrack: AudioTrack? = newAudioTrack(sampleRate)
        private val playThread = Thread({ loopPlay() }, "CosyVoicePcmPlayer")

        fun play() {
            if (!playThread.isAlive) playThread.start()
            finishSend = false
            playState = PlayState.PLAYING
            audioTrack?.play()
        }

        fun setAudioData(data: ByteArray) {
            if (playState != PlayState.RELEASED) {
                audioQueue.offer(data)
            }
        }

        fun finishSend() {
            finishSend = true
        }

        fun stop() {
            finishSend = false
            playState = PlayState.IDLE
            audioQueue.clear()
            runCatching {
                audioTrack?.flush()
                audioTrack?.pause()
                audioTrack?.stop()
            }
        }

        fun release() {
            stop()
            playState = PlayState.RELEASED
            runCatching { audioTrack?.release() }
            audioTrack = null
            playThread.interrupt()
        }

        fun setSampleRate(newSampleRate: Int) {
            if (sampleRate == newSampleRate && audioTrack != null) return
            sampleRate = newSampleRate
            runCatching { audioTrack?.release() }
            audioTrack = newAudioTrack(newSampleRate)
        }

        private fun loopPlay() {
            while (playState != PlayState.RELEASED) {
                if (playState != PlayState.PLAYING) {
                    sleepQuietly(20L)
                    continue
                }
                val data = audioQueue.poll()
                if (data == null) {
                    if (finishSend) {
                        finishSend = false
                        callback.onPlayOver()
                    } else {
                        sleepQuietly(10L)
                    }
                    continue
                }
                audioTrack?.write(data, 0, data.size)
            }
        }

        @Suppress("DEPRECATION")
        private fun newAudioTrack(sampleRate: Int): AudioTrack {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ) * 2
            return AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM,
            )
        }

        private fun sleepQuietly(timeMillis: Long) {
            runCatching { Thread.sleep(timeMillis) }
        }
    }
}
