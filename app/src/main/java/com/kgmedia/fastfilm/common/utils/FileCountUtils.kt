import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.zip.ZipInputStream
import java.util.regex.Pattern

object FileCountUtils {

    const val MAX_CHAR_COUNT = 500_000

    fun getSafeCharacterCount(context: Context?, uri: Uri): Int {
        if (context == null) return 0
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val inputStream = context.contentResolver.openInputStream(uri) ?: return 0

        return try {
            when {
                // 1. TXT 处理
                mimeType == "text/plain" -> countTxt(inputStream)

                // 2. Docx 处理 (基于 Zip 解析)
                uri.toString().endsWith(".docx") ||
                        mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                    countDocx(inputStream)
                }

                // 3. Doc 处理 (简单流过滤)
                uri.toString().endsWith(".doc") ||
                        mimeType == "application/msword" -> countDoc(inputStream)

                else -> 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    // TXT 逐块读取，防止大文件 OOM
    private fun countTxt(inputStream: InputStream): Int {
        var total = 0
        val reader = inputStream.bufferedReader() // 默认 UTF-8
        val buffer = CharArray(8192)
        var readLen: Int

        while (reader.read(buffer).also { readLen = it } != -1) {
            for (i in 0 until readLen) {
                val c = buffer[i]

                // 核心逻辑：模拟本地软件统计，排除换行符
                // '\r' (CR, ASCII 13) 和 '\n' (LF, ASCII 10)
                if (c != '\n' && c != '\r') {
                    total++
                }

                // 达到上限立即熔断，保护内存
                if (total > MAX_CHAR_COUNT) return MAX_CHAR_COUNT + 1
            }
        }
        return total
    }

    // Docx 核心解析逻辑
    private fun countDocx(inputStream: InputStream): Int {
        var total = 0
        val zis = ZipInputStream(inputStream)
        var entry = zis.nextEntry
        while (entry != null) {
            // 只读取 word/document.xml (存放正文的地方)
            if (entry.name == "word/document.xml") {
                val reader = zis.bufferedReader()
                val content = reader.readText()
                // 匹配 <w:t> 标签中的文字内容
                val matcher = Pattern.compile("<w:t.*?>(.*?)</w:t>").matcher(content)
                while (matcher.find()) {
                    val text = matcher.group(1) ?: ""
                    total += text.length
                    if (total > MAX_CHAR_COUNT) return MAX_CHAR_COUNT + 1
                }
                break
            }
            entry = zis.nextEntry
        }
        return total
    }

    // Doc 简单粗略估算 (过滤非打印字符)
    private fun countDoc(inputStream: InputStream): Int {
        var total = 0
        val reader = inputStream.bufferedReader(Charsets.ISO_8859_1)
        val buffer = CharArray(8192)
        var readLen: Int
        while (reader.read(buffer).also { readLen = it } != -1) {
            // 简单逻辑：只计算中文字符、字母和数字
            for (i in 0 until readLen) {
                val c = buffer[i]
                if (c.code in 0x4E00..0x9FA5 || c.isLetterOrDigit()) {
                    total++
                }
                if (total > MAX_CHAR_COUNT) return MAX_CHAR_COUNT + 1
            }
        }
        return total
    }
}