package com.kgmedia.fastfilm.common.utils

import android.content.Context
import android.net.Uri
import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PushbackInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object TextEncodingUtil {

    /**
     * 主方法：将用户选择的 TXT 转换为 UTF-8 编码的临时文件
     * * @param context 上下文
     * @param inputUri 用户选择的文件的 Uri (通常通过 SAF 获取)
     * @return 转换后的 UTF-8 格式的临时 File 对象，如果出错则返回 null
     */
    fun createUtf8TempFile(context: Context?, inputUri: Uri): File? {
        if (context == null) return null
        // 先生成临时文件的引用
        val tempFile = File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}.txt")

        return try {
            val originalEncoding = detectEncoding(context, inputUri) // 你的探测方法

            //使用底层的 Charset.forName 做严谨比对，无视各种别名差异
            val isUtf8 = try {
                Charset.forName(originalEncoding) == StandardCharsets.UTF_8
            } catch (e: Exception) {
                // 如果遇到不支持的编码名，降级为字符串比对
                originalEncoding.equals("UTF-8", ignoreCase = true)
            }

            //阻断 Null 安全陷阱！如果拿不到流，立刻终止，绝不返回假文件
            val inputStream = context.contentResolver.openInputStream(inputUri) ?: return null

            // 这里流一定不为空，放心使用 .use
            inputStream.use { stream ->
                FileOutputStream(tempFile).use { outputStream ->

                    if (isUtf8) {
                        val pushbackStream = PushbackInputStream(stream, 3)
                        val bom = ByteArray(3)
                        val bytesRead = pushbackStream.read(bom)

                        // 过滤 BOM 头逻辑保持不变，这是对的
                        if (bytesRead == 3 && bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && bom[2] == 0xBF.toByte()) {
                            pushbackStream.copyTo(outputStream)
                        } else {
                            if (bytesRead > 0) {
                                pushbackStream.unread(bom, 0, bytesRead)
                            }
                            pushbackStream.copyTo(outputStream)
                        }
                    } else {
                        // 正常做字符集转换
                        val reader = InputStreamReader(stream, originalEncoding)
                        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

                        val buffer = CharArray(4096)
                        var read: Int
                        while (reader.read(buffer).also { read = it } != -1) {
                            writer.write(buffer, 0, read)
                        }
                        writer.flush()
                    }
                }
            }

            //终极保险：确保文件真实存在才返回，否则删掉脏数据
            if (tempFile.exists()) {
                tempFile
            } else {
                tempFile.delete()
                null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // 发生异常时，清理可能写了一半的废文件
            if (tempFile.exists()) tempFile.delete()
            null
        }
    }

    /**
     * 探测文件编码
     * 为了内存安全，只读取前 8KB 数据进行识别
     */
    private fun detectEncoding(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val detector = UniversalDetector(null)
            val buffer = ByteArray(8192) // 每次读取 8KB
            var bytesRead: Int

            // 边读边喂给探测器，只要探测器没说 "探测完了"，就继续读
            while (inputStream.read(buffer).also { bytesRead = it } > 0 && !detector.isDone) {
                detector.handleData(buffer, 0, bytesRead)
            }
            // 告诉探测器数据结束了
            detector.dataEnd()

            // 获取探测结果
            val encoding = detector.detectedCharset
            detector.reset()

            // 如果识别出来了就返回，如果实在识别不出来，默认当做 UTF-8 处理
            return encoding ?: "UTF-8"
        }
        return "UTF-8"
    }
}