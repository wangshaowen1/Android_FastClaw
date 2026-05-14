package com.shiyuan.fastclaw.widget.progress

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableField
import com.shiyuan.fastclaw.R
import com.nhsd.basic.ext.dp
import kotlin.math.min

/**
 * 带中心文字的圆形进度指示器
 * 支持进度颜色、底色、文字内容和进度条宽度自定义
 */
class CircleProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 进度条宽度 (dp)
    var strokeWidth = 8.dp
        set(value) {
            field = value
            updatePaint()
            invalidate()
        }

    // 进度颜色
    var progressColor = 0xFF4CAF50.toInt()
        set(value) {
            field = value
            updatePaint()
            invalidate()
        }

    // 底色（背景圆环颜色）
    var ringBackgroundColor = 0xFFE0E0E0.toInt()
        set(value) {
            field = value
            updatePaint()
            invalidate()
        }

    // 中心文字
    var text: String = ""
        set(value) {
            field = value
            invalidate()
        }

    // 中心文字颜色
    var textColor = 0xFF333333.toInt()
        set(value) {
            field = value
            updatePaint()
            invalidate()
        }

    // 中心文字大小 (sp)
    var textSize = 14f
        set(value) {
            field = value
            updatePaint()
            invalidate()
        }

    // 当前进度 (0-100)
    var progress: Int = 0
        set(value) {
            val newProgress = value.coerceIn(0, 100)
            if (field != newProgress) {
                field = newProgress
                animateProgress(field, newProgress)
            }
        }

    // 动画时长 (ms)
    var animDuration = 500L

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val arcRect = RectF()
    private var animator: ValueAnimator? = null
    private var animatedProgress = 0f

    init {
        context.obtainStyledAttributes(attrs, R.styleable.CircleProgressView2).apply {
            try {
                strokeWidth = getDimensionPixelSize(
                    R.styleable.CircleProgressView2_cpv_strokeWidth, strokeWidth
                )
                progressColor = getColor(
                    R.styleable.CircleProgressView2_cpv_progressColor, progressColor
                )
                ringBackgroundColor = getColor(
                    R.styleable.CircleProgressView2_cpv_backgroundColor, ringBackgroundColor
                )
                text = getString(R.styleable.CircleProgressView2_cpv_text) ?: text
                textColor = getColor(
                    R.styleable.CircleProgressView2_cpv_textColor, textColor
                )
                textSize = getDimension(
                    R.styleable.CircleProgressView2_cpv_textSize, textSize.dp.toFloat()
                ) / context.resources.displayMetrics.scaledDensity
                progress = getInt(R.styleable.CircleProgressView2_cpv_progress, progress)
            } finally {
                recycle()
            }
        }
        updatePaint()
        animatedProgress = progress.toFloat()
    }

    private fun updatePaint() {
        backgroundPaint.apply {
            color = ringBackgroundColor
            strokeWidth = this@CircleProgressView.strokeWidth.toFloat()
        }
        progressPaint.apply {
            color = progressColor
            strokeWidth = this@CircleProgressView.strokeWidth.toFloat()
        }
        textPaint.apply {
            color = textColor
            textSize = this@CircleProgressView.textSize * context.resources.displayMetrics.scaledDensity
        }
    }

    private fun animateProgress(from: Int, to: Int) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(from.toFloat(), to.toFloat()).apply {
            duration = animDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animatedProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) - strokeWidth) / 2f

        // 绘制背景圆环
        arcRect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        canvas.drawArc(arcRect, 0f, 360f, false, backgroundPaint)

        // 绘制进度圆弧（从顶部 -90° 开始，顺时针）
        val sweepAngle = animatedProgress / 100f * 360f
        canvas.drawArc(arcRect, -90f, sweepAngle, false, progressPaint)

        // 绘制中心文字
        if (text.isNotEmpty()) {
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(text, centerX, textY, textPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }

    companion object {
        private const val TAG = "CircleProgressView"

        @JvmStatic
        @BindingAdapter("cpv_progressColor")
        fun setProgressColor(view: CircleProgressView, color: Int) {
            view.progressColor = color
        }

        @JvmStatic
        @BindingAdapter("cpv_backgroundColor")
        fun setBackgroundColor(view: CircleProgressView, color: Int) {
            view.ringBackgroundColor = color
        }

        @JvmStatic
        @BindingAdapter("cpv_progress")
        fun setProgress(view: CircleProgressView, progress: Int) {
            view.progress = progress
        }

        @JvmStatic
        @BindingAdapter("cpv_text")
        fun setText(view: CircleProgressView, text: String?) {
            view.text = text ?: ""
        }

        @JvmStatic
        @BindingAdapter("cpv_textColor")
        fun setTextColor(view: CircleProgressView, color: Int) {
            view.textColor = color
        }

        @JvmStatic
        @BindingAdapter("cpv_textSize")
        fun setTextSize(view: CircleProgressView, size: Int) {
            view.textSize = size.toFloat()
        }

        @JvmStatic
        @BindingAdapter("cpv_strokeWidth")
        fun setStrokeWidth(view: CircleProgressView, width: Int) {
            view.strokeWidth = width
        }
    }
}
