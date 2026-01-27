package com.kgmedia.fastfilm.kit.media.recorder

import android.util.Log
import com.nhsd.basic.manager.FileManager
import com.recordlife.audioprocessing.AutomaticGainControlUtils
import com.recordlife.audioprocessing.NoiseSuppressorUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object NoiseSuppressionHelper {

    val recordFile by lazy {
        File(FileManager.getInstance().audioFolder, "timbre_record_original.pcm")
    }

    private val outFile by lazy {
        File(FileManager.getInstance().audioFolder, "timbre_noise_suppression.pcm")
    }

    private val outWavFile by lazy {
        File(FileManager.getInstance().audioFolder, "noise_processing_timbre.wav")
    }

    // 自定义数据类，封装降噪和增益控制工具
    data class NoiseSuppressionTools(
        val nsUtils: NoiseSuppressorUtils?,
        val nsxId: Long,
        val agcUtils: AutomaticGainControlUtils?,
        val agcId: Long
    )

    fun processPcmFileWithNoiseSuppression(
        inputPcmPath: String?, // 输入的 PCM 文件
        sampleRate: Int = AudioRecordEngine.SAMPLE_RATE, // 采样率
    ): String? {
        // 初始化降噪和增益控制工具
        val tools = initializeNoiseSuppressionTools(sampleRate)
        if (inputPcmPath.isNullOrEmpty() || tools.nsUtils == null || tools.agcUtils == null) {
            Log.e("NoiseSuppression", "降噪或增益控制工具初始化失败")
            return null
        }
        // 处理 PCM 文件
        processPcmFile(File(inputPcmPath), outFile, tools)
        // 转换为 WAV 文件
        RecordUtils.makePCMFileToWAVFile(
            outFile.absolutePath,
            outWavFile.absolutePath,
            false
        )
        Log.d("NoiseSuppression", "PCM 文件处理完成，输出文件: ${outWavFile.absolutePath}")
        return outWavFile.absolutePath
    }

    private fun initializeNoiseSuppressionTools(
        sampleRate: Int
    ): NoiseSuppressionTools {
        var nsUtils: NoiseSuppressorUtils? = null
        var nsxId = 0L
        var agcUtils: AutomaticGainControlUtils? = null
        var agcId = 0L

        nsUtils = NoiseSuppressorUtils()
        nsxId = nsUtils.nsxCreate()
        if (nsxId == 0L) {
            Log.e("NoiseSuppression", "降噪工具初始化失败")
            return NoiseSuppressionTools(null, 0, null, 0)
        }
        val nsxInit = nsUtils.nsxInit(nsxId, sampleRate)
        val nexSetPolicy = nsUtils.nsxSetPolicy(nsxId, 2) // 设置降噪强度
        Log.i("NoiseSuppression", "nsxId: $nsxId, nsxInit: $nsxInit, nexSetPolicy: $nexSetPolicy")

        agcUtils = AutomaticGainControlUtils()
        agcId = agcUtils.agcCreate()
        if (agcId == 0L) {
            Log.e("NoiseSuppression", "增益控制工具初始化失败")
            return NoiseSuppressionTools(null, 0, null, 0)
        }
        val agcInitResult = agcUtils.agcInit(agcId, 0, 255, 3, sampleRate)
        val agcSetConfigResult = agcUtils.agcSetConfig(agcId, 9, 9, true)
        Log.e(
            "NoiseSuppression",
            "agcId: $agcId, agcInit: $agcInitResult, agcSetConfig: $agcSetConfigResult"
        )

        return NoiseSuppressionTools(nsUtils, nsxId, agcUtils, agcId)
    }

    private fun processPcmFile(
        inputPcmFile: File,
        outputPcmFile: File,
        tools: NoiseSuppressionTools,
    ) {
        val bufferSize = 320  //固定值，不能改！！！！！！！！
        val buffer = ByteArray(bufferSize)
        val inputData = ShortArray(bufferSize / 2) // 输入数据（480 个样本）
        val outNsData = ShortArray(bufferSize / 2) // 降噪后的数据
        val outAgcData = ShortArray(bufferSize / 2) // 增益控制后的数据

        FileInputStream(inputPcmFile).use { inputStream ->
            FileOutputStream(outputPcmFile).use { outputStream ->
                while (true) {
                    // 从文件中读取 PCM 数据
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) {
                        break // 文件读取完毕
                    }
                    // 将字节数据转换为 short 数组
                    ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(inputData)
                    // 降噪处理
                    tools.nsUtils?.nsxProcess(tools.nsxId, inputData, 1, outNsData)
                    // 增益控制处理
                    tools.agcUtils?.agcProcess(tools.agcId, outNsData, 1, inputData.size, outAgcData, 0, 0, 0, false)
                    // 将处理后的数据写入输出文件
                    val outputBuffer = ByteArray(outAgcData.size * 2)
                    ByteBuffer.wrap(outputBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outAgcData)
                    outputStream.write(outputBuffer)

                }
            }
        }
        // 释放资源
        tools.nsUtils?.nsxFree(tools.nsxId)
        tools.agcUtils?.agcFree(tools.agcId)
    }
}