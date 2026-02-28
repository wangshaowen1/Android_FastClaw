package com.kgmedia.fastfilm.common.utils

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.text.Html
import kotlinx.parcelize.Parcelize
import java.io.InputStream
import java.util.zip.ZipInputStream
import java.util.regex.Pattern
import kotlin.math.ceil

object FileCountUtils {

    // 保留你原本的 50万字 熔断上限
    const val MAX_CHAR_COUNT = 500_000
    // 恶意长词拆分阈值
    private const val MAX_WORD_LENGTH = 10

    /**
     * 分析结果：同时包含 字符数、Token数、预估算力
     */
    @Parcelize
    data class AnalysisResult(
        val charCount: Int,
        val tokenCount: Int,
        val estimatedPower: Double
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
        // 使用 readLine 逐行读取，代替字符数组，更方便处理分词
        while (reader.readLine().also { line = it } != null) {
            analyzeString(line!!, stats)
            // 熔断保护：字数超限直接终止
            if (stats.charCount > MAX_CHAR_COUNT) {
                stats.charCount = MAX_CHAR_COUNT + 1
                break
            }
        }
    }

    private fun analyzeDocx(inputStream: InputStream, stats: TextStats) {
        val zis = ZipInputStream(inputStream)
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val content = zis.bufferedReader().readText()
                val matcher = Pattern.compile("<w:t.*?>(.*?)</w:t>").matcher(content)
                while (matcher.find()) {
                    val rawText = matcher.group(1) ?: ""
                    // XML 转义符还原
                    val decodedText = Html.fromHtml(rawText, Html.FROM_HTML_MODE_LEGACY).toString()
                    analyzeString(decodedText, stats)

                    if (stats.charCount > MAX_CHAR_COUNT) {
                        stats.charCount = MAX_CHAR_COUNT + 1
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
        var readLen: Int
        while (reader.read(buffer).also { readLen = it } != -1) {
            analyzeString(String(buffer, 0, readLen), stats)
            if (stats.charCount > MAX_CHAR_COUNT) {
                stats.charCount = MAX_CHAR_COUNT + 1
                break
            }
        }
    }

    /**
     * 核心双轨统计算法：同时计算字数(char)和Token
     */
    private fun analyzeString(text: String, stats: TextStats) {
        var tokens = 0
        var chars = 0
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                // 1. 换行符：不计入原文字数，也不计入 Token
                c == '\n' || c == '\r' -> {
                    i++
                }

                // 2. 中文、标点、空格：字数+1，Token+1
                isChinese(c) || isPunctuationOrSpace(c) -> {
                    chars++
                    tokens++
                    i++
                }

                // 3. 英文与数字块：字数按实际字母数加，Token按块加
                c.isLetterOrDigit() -> {
                    val start = i
                    while (i < text.length && text[i].isLetterOrDigit()) {
                        i++
                    }
                    val len = i - start

                    chars += len // 原文字数累加真实长度（如 word 是 4个字符）

                    // Token 累加按块折算（1块=1个Token，超长按阈值拆分）
                    tokens += if (len <= MAX_WORD_LENGTH) {
                        1
                    } else {
                        ceil(len.toDouble() / MAX_WORD_LENGTH).toInt()
                    }
                }

                // 4. 兜底
                else -> {
                    chars++
                    tokens++
                    i++
                }
            }
        }
        // 累加到全局统计对象中
        stats.charCount += chars
        stats.tokenCount += tokens
    }

    private fun isChinese(c: Char): Boolean = c.code in 0x4E00..0x9FA5

    private fun isPunctuationOrSpace(c: Char): Boolean {
        return c.isWhitespace() || (!c.isLetterOrDigit() && !isChinese(c))
    }

    /**
     * 计算预估算力值
     */
    fun calculateEstimatedPower(tokenCount: Int): Double {
        return (tokenCount.toDouble() / 1_000_000.0) * 50.0
    }
}