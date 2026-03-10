package com.kgmedia.fastfilm.common.utils

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.text.Html
import com.kgmedia.fastfilm.core.Config
import kotlinx.parcelize.Parcelize
import java.io.InputStream
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

object FileCountUtils {

    // 保留你原本的 50万字 熔断上限
    val MAX_CHAR_COUNT: Int
        get() = Config.get().appConfig.workNovelWordLimit ?: 500000

    /**
     * 分析结果：同时包含 字符数、Token数、预估算力
     */
    @Parcelize
    data class AnalysisResult(
        val charCount: Int,
        val tokenCount: Int,
        var estimatedPower: Double
    ) : Parcelable

    // 内部累加器，用于在流式读取时同时统计两项数据
    private class TextStats(var charCount: Int = 0, var tokenCount: Int = 0)

    fun getFileAnalysis(context: Context?, uri: Uri): AnalysisResult {
        if (context == null) return AnalysisResult(0, 0, 0.0)
        val mimeType = context.contentResolver.getType(uri) ?: ""

        val stats = TextStats()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                when {
                    mimeType == "text/plain" -> analyzeTxt(inputStream, stats)

                    uri.toString().endsWith(".docx") ||
                            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                        analyzeDocx(inputStream, stats)
                    }

                    uri.toString().endsWith(".doc") ||
                            mimeType == "application/msword" -> analyzeDoc(inputStream, stats)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return AnalysisResult(
            charCount = stats.charCount,
            tokenCount = stats.tokenCount,
            estimatedPower = calculateEstimatedPower(stats.tokenCount)
        )
    }

    private fun analyzeTxt(inputStream: InputStream, stats: TextStats) {
        val reader = inputStream.bufferedReader()
        var line: String?
        val limit = MAX_CHAR_COUNT
        // 使用 readLine 逐行读取，自带去除了换行符的作用
        while (reader.readLine().also { line = it } != null) {
            analyzeString(line!!, stats)
            // 熔断保护：字数超限直接终止
            if (stats.charCount > limit) {
                stats.charCount = limit + 1
                stats.tokenCount = stats.charCount // 保持 Token 和字符同步
                break
            }
        }
    }

    private fun analyzeDocx(inputStream: InputStream, stats: TextStats) {
        val zis = ZipInputStream(inputStream)
        var entry = zis.nextEntry
        val limit = MAX_CHAR_COUNT
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val content = zis.bufferedReader().readText()
                val matcher = Pattern.compile("<w:t.*?>(.*?)</w:t>").matcher(content)
                while (matcher.find()) {
                    val rawText = matcher.group(1) ?: ""
                    // XML 转义符还原
                    val decodedText = Html.fromHtml(rawText, Html.FROM_HTML_MODE_LEGACY).toString()
                    analyzeString(decodedText, stats)

                    if (stats.charCount > limit) {
                        stats.charCount = limit + 1
                        stats.tokenCount = stats.charCount // 保持 Token 和字符同步
                        break
                    }
                }
                break
            }
            entry = zis.nextEntry
        }
    }

    private fun analyzeDoc(inputStream: InputStream, stats: TextStats) {
        val reader = inputStream.bufferedReader(Charsets.ISO_8859_1)
        val buffer = CharArray(8192)
        val limit = MAX_CHAR_COUNT
        var readLen: Int
        while (reader.read(buffer).also { readLen = it } != -1) {
            analyzeString(String(buffer, 0, readLen), stats)
            if (stats.charCount > limit) {
                stats.charCount = limit + 1
                stats.tokenCount = stats.charCount // 保持 Token 和字符同步
                break
            }
        }
    }

    /**
     * 核心统计算法：极简模式，所有字符都算一个 Token。
     */
    private fun analyzeString(text: String, stats: TextStats) {
        // 兼容你原先的逻辑：剔除 '\n' 和 '\r'。
        // 如果现在连换行符也要算作1个字/token，可以直接改成：val count = text.length
        val count = text.count { it != '\n' && it != '\r' }

        stats.charCount += count
        stats.tokenCount += count
    }

    /**
     * 计算预估算力值
     */
    fun calculateEstimatedPower(tokenCount: Int): Double {
        return (tokenCount.toDouble() / 1_000_000.0) * 50.0
    }
}