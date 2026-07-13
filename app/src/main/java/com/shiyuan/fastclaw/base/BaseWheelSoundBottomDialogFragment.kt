package com.shiyuan.fastclaw.base

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.view.HapticFeedbackConstants
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
    protected open val wheelHapticEnabled: Boolean = true
    protected open val wheelVibrateFallbackMs: Long = 8L

    private var wheelSoundPool: SoundPool? = null
    private var wheelSoundId = 0
    private var wheelSoundLoaded = false
    private var lastWheelSoundTime = 0L
    private var vibrator: Vibrator? = null
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
                playWheelFeedback(view)
            }
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                bindWheelSound(view.getChildAt(index))
            }
        }
    }

    private fun playWheelFeedback(view: View) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastWheelSoundTime < wheelSoundIntervalMs) {
            return
        }
        lastWheelSoundTime = now
        if (wheelHapticEnabled) {
            playWheelHaptic(view)
        }
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

    private fun playWheelHaptic(view: View) {
        val performed = view.performHapticFeedback(
            HapticFeedbackConstants.CLOCK_TICK,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
        if (performed) {
            return
        }
        val vibrator = getWheelVibrator() ?: return
        if (!vibrator.hasVibrator()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    wheelVibrateFallbackMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(wheelVibrateFallbackMs)
        }
    }

    private fun getWheelVibrator(): Vibrator? {
        vibrator?.let { return it }
        val service = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator = service
        return service
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
        vibrator = null
        boundPickers.clear()
        super.onDestroy()
    }
}
