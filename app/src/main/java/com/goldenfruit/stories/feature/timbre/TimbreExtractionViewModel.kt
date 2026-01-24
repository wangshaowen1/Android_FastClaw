package com.kgmedia.fastfilm.feature.timbre

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.App
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.base.viewmodel.ToolbarViewModel
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.Config
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.feature.timbre.TimbreExtractionActivity.Companion.EXTRA_TIMBRE_ID
import com.kgmedia.fastfilm.feature.timbre.data.TimbreRepository
import com.kgmedia.fastfilm.kit.recorder.AudioRecordEngine
import com.kgmedia.fastfilm.kit.recorder.NoiseSuppressionHelper
import com.kgmedia.fastfilm.kit.recorder.RecorderEngine.RecorderEngineListener
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger

/**
 * 人声提取
 */
class TimbreExtractionViewModel : AppRepositoryViewModel<TimbreRepository>() {
    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        background = Color.TRANSPARENT,
        title = ""
    )

    val timbreId by lazy { mBundle.getString(EXTRA_TIMBRE_ID) }

    val displayChild = ObservableInt(1)
    var recordDuring = 0L
    val recordTime = ObservableField("00:00")
    val recordProgress = ObservableInt()
    val recording = ObservableBoolean(false)
    val recordPause = ObservableBoolean(false)
    val audioPath = ObservableField<String>()
    private var minRecordDuring = 30
    var maxRecordDuring = ObservableInt(60)
    var commitEnable = ObservableBoolean(false)

    private val recordEngine by lazy {
        AudioRecordEngine()
    }

    private fun getRecordFilePath(): String {
        return NoiseSuppressionHelper.recordFile.absolutePath
    }

    override fun onCreate() {
        super.onCreate()
        Config.get().appConfig.cloneTimbreOriginalVocalsDurationLimit.split("-").apply {
            if (this.size > 1) {
                minRecordDuring = this[0].toInt()
                maxRecordDuring.set(this[1].toInt())
            }
        }
        recordEngine.during = maxRecordDuring.get()
        recordEngine.setRecorderListener(object : RecorderEngineListener {
            override fun onRecorderStart() {
                Logger.d("录制开始")
                recordDuring = 0L
                recordPause.set(false)
                recording.set(true)
            }

            override fun onRecorderStop() {
                Logger.d("录制停止  recordDuring=${recordDuring}}")
                recordTime.set(DateUtil.secToTimeMis(recordDuring))
                recording.set(false)
            }

            override fun onRecorderPause() {
                Logger.d("录制暂停")
                recordPause.set(true)
            }

            override fun onRecorderResume() {
                Logger.d("录制恢复")
                recordPause.set(false)
            }

            override fun onRecorderProgress(millis: Long) {
                recordDuring = millis
                commitEnable.set(millis >= minRecordDuring * 1000L)
                recordProgress.set((millis * 100 / (maxRecordDuring.get() * 1000L)).toInt())
                recordTime.set(DateUtil.secToTimeMis(millis))
            }

            override fun onRecorderStreamError(e: Throwable) {
                Logger.d("录制出错  ${e.fillInStackTrace()}")
                launch {
                    "录制失败，请重录~".showToast()
                    reset()
                }
            }
        })
        recordEngine.setSoundAmplitudeListener { ratio, db, value ->
            Logger.d("监测录制 ratio=${ratio}  db=${db}   value=${value}")
        }
    }

    private fun reset() {
        recording.set(false)
        recordProgress.set(0)
        audioPath.set(null)
        commitEnable.set(false)
        recordTime.set(DateUtil.secToTimeMis(0L))
    }

    val handler by lazy { Handler(Looper.getMainLooper()) }
    private var countDownTime = 3
    val countDownTimeShow by lazy { ObservableInt(0) }
    val runnable: Runnable by lazy {
        Runnable {
            countDownTimeShow.set(countDownTime)
            if (countDownTime > 0) {
                countDownTime--
                handler.postDelayed(runnable, 1000)
            } else {
                handler.removeCallbacks(runnable)
                audioPath.set(getRecordFilePath())
                recordEngine.startRecord(audioPath.get())
                displayChild.set(0)
            }
        }
    }

    fun startRecord() {
        countDownTime = 0
        handler.post(runnable)
    }

    fun stopRecord() {
        Logger.d("录制主动停止  recording.get()=${recording.get()}")
        if (recording.get()) {
            recordEngine.stop()
        }
    }

    fun pauseRecord() {
        recordEngine.pause()
    }

    fun resumeRecord() {
        if (recordDuring >= maxRecordDuring.get() * 1000L) {
            return
        }
        recordEngine.resume()
    }

    fun timbreCloneGenerate(path: String?, function: (String?) -> Unit) {
        setLoading(true)
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(path, OssClient.TYPE_AUDIO, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val url = uploadSuccess.firstOrNull()
                if (url.isNullOrEmpty()) {
                    onUploadFail(null)
                } else {
                    launch {
                        val response =
                            mRepo.timbreAddOrOptimize(url, timbreId)
                        if (response.isSuccess) {
                            reset()
                            function.invoke(response.data?.timbreId)
                        } else {
                            response.errorMsg?.showToast()
                        }
                        setLoading(false)
                    }
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                setLoading(false)
                "上传失败".showToast()
            }
        })
    }

    override fun onCleared() {
        recordEngine.stop()
        handler.removeCallbacks(runnable)
        super.onCleared()
    }

}