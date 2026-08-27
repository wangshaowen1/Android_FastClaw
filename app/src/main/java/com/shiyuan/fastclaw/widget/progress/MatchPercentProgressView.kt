package com.shiyuan.fastclaw.widget.progress

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.shiyuan.fastclaw.R
import kotlin.math.min

/**
 * 展示匹配度数值的圆形进度控件。
 */
class MatchPercentProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var progress: Int = 0
        set(value) {
            val newProgress = value.coerceIn(0, MAX_PROGRESS)
            if (field == newProgress) return
            field = newProgress
            contentDescription = progressText(newProgress)
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private val strokeWidth = STROKE_WIDTH_DP * density
    private val arcRect = RectF()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_e9f0fe)
        style = Paint.Style.STROKE
        strokeWidth = this@MatchPercentProgressView.strokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@MatchPercentProgressView.strokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    private val numberPaint = createTextPaint(NUMBER_TEXT_SIZE_SP)
    private val percentPaint = createTextPaint(PERCENT_TEXT_SIZE_SP)

    init {
        contentDescription = progressText(0)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        progressPaint.shader = LinearGradient(
            strokeWidth / 2f,
            height / 2f,
            width - strokeWidth / 2f,
            height / 2f,
            ContextCompat.getColor(context, R.color.color_336fff),
            ContextCompat.getColor(context, R.color.color_2fc8ff),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) - strokeWidth) / 2f
        arcRect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
        )

        canvas.drawCircle(centerX, centerY, radius, fillPaint)
        canvas.drawArc(arcRect, 0f, FULL_ANGLE, false, backgroundPaint)

        if (progress > 0) {
            val sweepAngle = progress / MAX_PROGRESS.toFloat() * FULL_ANGLE
            canvas.drawArc(arcRect, -sweepAngle, sweepAngle, false, progressPaint)
        }

        drawProgressText(canvas, centerX, centerY)
    }

    private fun drawProgressText(canvas: Canvas, centerX: Float, centerY: Float) {
        val number = context.getString(
            R.string.processed_opportunity_match_percent_value,
            progress,
        )
        val percent = context.getString(R.string.percent_symbol)
        val numberWidth = numberPaint.measureText(number)
        val percentWidth = percentPaint.measureText(percent)
        val textWidth = numberWidth + percentWidth
        val availableTextWidth = min(width, height) - strokeWidth * 2f
        val textScale = min(1f, availableTextWidth / textWidth)
        val startX = centerX - textWidth / 2f

        canvas.save()
        canvas.scale(textScale, textScale, centerX, centerY)
        canvas.drawText(number, startX, centerTextBaseline(centerY, numberPaint), numberPaint)
        canvas.drawText(
            percent,
            startX + numberWidth,
            centerTextBaseline(centerY, percentPaint),
            percentPaint,
        )
        canvas.restore()
    }

    private fun createTextPaint(textSizeSp: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_90_000000)
        textSize = textSizeSp * scaledDensity
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun centerTextBaseline(centerY: Float, paint: Paint): Float =
        centerY - (paint.ascent() + paint.descent()) / 2f

    private fun progressText(value: Int): String =
        context.getString(R.string.processed_opportunity_match_percent_value, value) +
            context.getString(R.string.percent_symbol)

    private companion object {
        const val MAX_PROGRESS = 100
        const val FULL_ANGLE = 360f
        const val STROKE_WIDTH_DP = 2f
        const val NUMBER_TEXT_SIZE_SP = 15f
        const val PERCENT_TEXT_SIZE_SP = 10f
    }
}
