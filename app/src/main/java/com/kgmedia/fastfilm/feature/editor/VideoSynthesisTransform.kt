package com.kgmedia.fastfilm.feature.editor

import com.kgmedia.fastfilm.core.network.RetrofitClient
import com.kuaige.videoedit.ffmpeg.BaseFFmpegTransform
import com.kuaige.videoedit.ffmpeg.FFmpegCommandBuilder
import com.kuaige.videoedit.resource.Resource
import com.kuaige.videoedit.resource.UrlResource
import java.io.File

/**
 * 音乐视频 (FFmpeg 实现)
 */
class VideoSynthesisTransform(
    storyboards: List<String>
) : BaseFFmpegTransform() {

    override val resourceManager by lazy { VideoSynthesisResourceManager(storyboards.map { UrlResource(it, RetrofitClient.httpClient) }) }

    override val outputFileName: String
        get() = "VideoSynthesis_${System.nanoTime()}.mp4"

    override suspend fun generateCommand(outputFile: File): Pair<String, Long> {
        // 资源校验：此时所有 Resource 应该都已经下载完成了
        val videos = resourceManager.storyboards?.takeIf { it.isNotEmpty() }
            ?: throw kotlin.IllegalStateException("Video materials missing")
        val builder = FFmpegCommandBuilder()
        val fileListFile = generateConcatFile(videos, outputFile)
        builder.addInput(fileListFile.absolutePath, "-f concat -safe 0")
        val extra = "-c:v libx264 -preset veryfast -r 30 -pix_fmt yuv420p"
        return builder.build(outputFile.absolutePath, extra) to 300
    }

    private fun generateConcatFile(videos: List<Resource>, outputFile: File): File {
        val sb = kotlin.text.StringBuilder()
        for (video in videos) {
            sb.append("file '${video.file.absolutePath}'\n")
        }
        val fileListFile = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_file_list.txt")
        fileListFile.writeText(sb.toString())
        return fileListFile
    }
}