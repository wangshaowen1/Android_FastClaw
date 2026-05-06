package com.shiyuan.fastclaw.kit.media

import android.media.MediaPlayer
import androidx.annotation.RawRes
import com.shiyuan.fastclaw.App
import com.shiyuan.fastclaw.R

object AudioPlayer {

    private var player: MediaPlayer? = null

    /**
     * 播放金币到账音效
     */
    fun playCoinAudio() {
//        playAudio(R.raw.audio_receive_coin)
    }

    /**
     * 播放语音
     */
    fun playAudio(@RawRes resId: Int) {
        release()
        val player = MediaPlayer.create(App.instance, resId)
        AudioPlayer.player = player
        player?.setOnCompletionListener { release() }
        player?.start()
    }

    private fun release() {
        player?.release()
        player = null
    }
}