package com.shiyuan.fastclaw.base

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.flqy.datepicker.NumberPicker
import com.shiyuan.fastclaw.R

/**
 * 底部滚轮弹窗基类：自动给内部 NumberPicker 增加滚动齿轮音效。
 */
abstract class BaseWheelSoundBottomDialogFragment<V : ViewBinding> : BaseBottomDialogFragment<V>() {

    protected open val wheelSoundIntervalMs: Long = 20L
    protected open val wheelSoundVolume: Float = 0.5f

    private var wheelSoundPool: SoundPool? = null
    private var wheelSoundId = 0
    private var wheelSoundLoaded = false
    private var lastWheelSoundTime = 0L
    private val boundPickers = mutableSetOf<NumberPicker>()

    override fun onPrepareView() {
        super.onPrepareView()
        initWheelSound()
        binding.root.post {
            bindWheelSound(binding.root)
        }
    }

    private fun bindWheelSound(view: View) {
        if (view is NumberPicker && boundPickers.add(view)) {
            view.addOnValueChangedListener { _, _, _ ->
                playWheelSound()
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                bindWheelSound(view.getChildAt(index))
            }
        }
    }

    private fun playWheelSound() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastWheelSoundTime < wheelSoundIntervalMs) {
            return
        }
        lastWheelSoundTime = now
        if (!wheelSoundLoaded || wheelSoundId == 0) {
            return
        }
        wheelSoundPool?.play(
            wheelSoundId,
            wheelSoundVolume,
            wheelSoundVolume,
            1,
            0,
            1f
        )
    }

    private fun initWheelSound() {
        if (wheelSoundPool != null) {
            return
        }
        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        pool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                wheelSoundLoaded = true
            }
        }
        wheelSoundPool = pool
        wheelSoundId = pool.load(requireContext(), R.raw.wheel_gear_tick, 1)
    }

    override fun onDestroy() {
        wheelSoundPool?.release()
        wheelSoundPool = null
        wheelSoundId = 0
        wheelSoundLoaded = false
        boundPickers.clear()
        super.onDestroy()
    }
}
