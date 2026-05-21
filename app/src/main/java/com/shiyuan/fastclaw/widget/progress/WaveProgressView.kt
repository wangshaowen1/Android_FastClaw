package com.shiyuan.fastclaw.widget.progress

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.databinding.BindingAdapter
import com.nhsd.basic.ext.dp
import com.shiyuan.fastclaw.R
import kotlin.math.min
import kotlin.math.sin

/**
 * 波浪进度条控件
 * 支持波浪动画效果，可设置进度、文字内容、文字大小和颜色
 */
class WaveProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 进度值 (0-100)
    var progress: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    // 波浪颜色
    var waveColor = 0xFF4CAF50.toInt()
        set(value) {
            field = value
            wavePaint.color = waveColor
            invalidate()
        }

    // 波浪背景色
    var waveBackgroundColor = 0xFFE0E0E0.toInt()
        set(value) {
            field = value
            bgPaint.color = waveBackgroundColor
            invalidate()
        }

    // 波浪透明度 (0-255)
    var waveAlpha = 180
        set(value) {
            field = value.coerceIn(0, 255)
            wavePaint.alpha = waveAlpha
            invalidate()
        }

    // 中心文字
    var text: String = ""
        set(value) {
            field = value
            invalidate()
        }

    // 中心文字颜色
    var textColor = 0xFFFFFFFF.toInt()
        set(value) {
            field = value
            textPaint.color = textColor
            invalidate()
        }

    // 中心文字大小 (sp)
    var textSize = 16f
        set(value) {
            field = value
            textPaint.textSize = value * context.resources.displayMetrics.scaledDensity
            invalidate()
        }

    // 波浪振幅 (px)
    var waveAmplitude = 20f
        set(value) {
            field = value
            invalidate()
        }

    // 波浪周期数量
    var waveCount = 1f
        set(value) {
            field = value
            invalidate()
        }

    // 波浪移动速度
    var waveSpeed = 100f
        set(value) {
            field = value
            waveAnimator?.duration = (3000 / waveSpeed * 100).toLong()
        }

    // 是否显示波浪动画
    var isWaveAnimEnabled = true
        set(value) {
            field = value
            if (value) {
                waveAnimator?.start()
            } else {
                waveAnimator?.cancel()
            }
        }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val clipPath = Path()
    private val wavePath = Path()

    private var wavePhase = 0f
    private var waveAnimator: ValueAnimator? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.WaveProgressView).apply {
            try {
                progress = getInt(R.styleable.WaveProgressView_wpv_progress, progress)
                waveColor = getColor(R.styleable.WaveProgressView_wpv_waveColor, waveColor)
                waveBackgroundColor = getColor(R.styleable.WaveProgressView_wpv_waveBackgroundColor, waveBackgroundColor)
                waveAlpha = getInt(R.styleable.WaveProgressView_wpv_waveAlpha, waveAlpha)
                text = getString(R.styleable.WaveProgressView_wpv_text) ?: text
                textColor = getColor(R.styleable.WaveProgressView_wpv_textColor, textColor)
                textSize = getDimension(R.styleable.WaveProgressView_wpv_textSize, textSize.dp.toFloat()) /
                        context.resources.displayMetrics.scaledDensity
                waveAmplitude = getDimension(R.styleable.WaveProgressView_wpv_waveAmplitude, 20f.dp.toFloat())
                waveCount = getFloat(R.styleable.WaveProgressView_wpv_waveCount, waveCount)
                waveSpeed = getFloat(R.styleable.WaveProgressView_wpv_waveSpeed, waveSpeed.toFloat())
                isWaveAnimEnabled = getBoolean(R.styleable.WaveProgressView_wpv_waveAnimEnabled, isWaveAnimEnabled)
            } finally {
                recycle()
            }
        }

        wavePaint.color = waveColor
        wavePaint.alpha = waveAlpha
        bgPaint.color = waveBackgroundColor
        textPaint.color = textColor
        textPaint.textSize = textSize * context.resources.displayMetrics.scaledDensity

        setupWaveAnimator()
    }

    private fun setupWaveAnimator() {
        waveAnimator = ValueAnimator.ofFloat(0f, 2 * Math.PI.toFloat()).apply {
            duration = (3000 / waveSpeed * 100).toLong()
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                wavePhase = animation.animatedValue as Float
                invalidate()
            }
        }
        if (isWaveAnimEnabled) {
            waveAnimator?.start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = minOf(width, height) / 2f
        val centerX = width / 2f
        val centerY = height / 2f

        // 绘制圆形背景
        canvas.drawCircle(centerX, centerY, radius, bgPaint)

        // 计算水位高度（从底部往上）
        val waterLevel = height * (1 - progress / 100f)

        // 创建圆形裁剪区域
        clipPath.reset()
        clipPath.addCircle(centerX, centerY, radius, Path.Direction.CW)

        canvas.save()
        canvas.clipPath(clipPath)

        // 绘制波浪
        wavePath.reset()
        val waveLength = width / waveCount
        wavePath.moveTo(0f, height)

        // 绘制第一层波浪
        var x = 0f
        while (x <= width) {
            val y = waterLevel + waveAmplitude * sin((x / waveLength * 2 * Math.PI + wavePhase).toDouble()).toFloat()
            if (x == 0f) {
                wavePath.moveTo(x, y)
            } else {
                wavePath.lineTo(x, y)
            }
            x += 1f
        }

        wavePath.lineTo(width, height)
        wavePath.lineTo(0f, height)
        wavePath.close()

        // 绘制第二层波浪（略有偏移，颜色更浅）
        val wavePaint2 = Paint(wavePaint).apply {
            alpha = (waveAlpha * 0.6f).toInt()
        }
        x = 0f
        val wavePath2 = Path()
        wavePath2.moveTo(0f, height)
        while (x <= width) {
            val y = waterLevel + waveAmplitude * sin((x / waveLength * 2 * Math.PI + wavePhase + Math.PI / 2).toDouble()).toFloat()
            if (x == 0f) {
                wavePath2.moveTo(x, y)
            } else {
                wavePath2.lineTo(x, y)
            }
            x += 1f
        }
        wavePath2.lineTo(width, height)
        wavePath2.lineTo(0f, height)
        wavePath2.close()

        canvas.drawPath(wavePath2, wavePaint2)
        canvas.drawPath(wavePath, wavePaint)

        canvas.restore()

        // 绘制中心文字
        if (text.isNotEmpty()) {
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(text, centerX, textY, textPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator?.cancel()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isWaveAnimEnabled && waveAnimator?.isRunning != true) {
            waveAnimator?.start()
        }
    }

    companion object {
        private const val TAG = "WaveProgressView"

        @JvmStatic
        @BindingAdapter("wpv_progress")
        fun setProgress(view: WaveProgressView, progress: Int) {
            view.progress = progress
        }

        @JvmStatic
        @BindingAdapter("wpv_waveColor")
        fun setWaveColor(view: WaveProgressView, color: Int) {
            view.waveColor = color
        }

        @JvmStatic
        @BindingAdapter("wpv_waveBackgroundColor")
        fun setWaveBackgroundColor(view: WaveProgressView, color: Int) {
            view.waveBackgroundColor = color
        }

        @JvmStatic
        @BindingAdapter("wpv_text")
        fun setText(view: WaveProgressView, text: String?) {
            view.text = text ?: ""
        }

        @JvmStatic
        @BindingAdapter("wpv_textColor")
        fun setTextColor(view: WaveProgressView, color: Int) {
            view.textColor = color
        }

        @JvmStatic
        @BindingAdapter("wpv_textSize")
        fun setTextSize(view: WaveProgressView, sizeSp: Int) {
            view.textSize = sizeSp.toFloat()
        }

        @JvmStatic
        @BindingAdapter("wpv_waveAnimEnabled")
        fun setWaveAnimEnabled(view: WaveProgressView, enabled: Boolean) {
            view.isWaveAnimEnabled = enabled
        }

        @JvmStatic
        @BindingAdapter("wpv_waveAlpha")
        fun setWaveAlpha(view: WaveProgressView, alpha: Int) {
            view.waveAlpha = alpha
        }
    }
}
