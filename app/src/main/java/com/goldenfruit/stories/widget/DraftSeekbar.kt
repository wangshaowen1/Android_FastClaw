package com.kgmedia.fastfilm.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import com.kgmedia.fastfilm.common.ext.formatDuration
import com.kgmedia.fastfilm.common.ext.updateLayoutParams
import com.kgmedia.fastfilm.databinding.LayoutUnpublishedSeekbarBinding
import com.nhsd.basic.ext.dp

/**
 * 未发布播放器进度条
 */
class DraftSeekbar : FrameLayout {

    var currentPosition = 0L
        set(value) {
            if (field != value) {
                field = value
                update()
            }
        }
    var duration = 0L
        set(value) {
            if (value >= 0) {
                field = value
                update()
            }
        }
    var onSeekListener: OnSeekListener? = null

    /**
     * 拖动状态
     */
    private var dragging = false
        set(value) {
            if (field != value) {
                requestDisallowInterceptTouchEvent(value)
                if (value) {
                    onSeekListener?.onDrag()
                    binding.progressBg.updateLayoutParams<LayoutParams> { it.height = 4.dp }
                    binding.thumb.setPadding(12.dp)
                } else {
                    onSeekListener?.onDragEnd(dragValue)
                    binding.progressBg.updateLayoutParams<LayoutParams> { it.height = 2.dp }
                    binding.thumb.setPadding(17.dp)
                    currentPosition = dragValue
                }
                field = value
            }
        }
    private var dragValue = 0L
        set(value) {
            if (field != value) {
                field = value
                update()
            }
        }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private val binding by lazy { LayoutUnpublishedSeekbarBinding.inflate(LayoutInflater.from(context), this, true) }

    private fun init(context: Context, attrs: AttributeSet?) {
        binding
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                onTouch(event)
                dragging = true
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                onTouch(event)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onTouch(event)
                dragging = false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun onTouch(event: MotionEvent) {
        dragValue = (((event.x - binding.progressBg.x) / binding.progressBg.width).coerceAtMost(1f).coerceAtLeast(0f) * duration).toLong()
    }

    private fun update() {
        if (duration < 1) {
            binding.tvProgress.text = 0L.formatDuration()
            binding.progress.updateLayoutParams<ConstraintLayout.LayoutParams> {
                it.horizontalBias = 0f
            }
        } else {
            val position = if (dragging) dragValue else currentPosition
            binding.tvProgress.text = position.formatDuration()
            binding.progress.updateLayoutParams<ConstraintLayout.LayoutParams> {
                it.horizontalBias = (1f * position / duration).coerceAtMost(1f)
            }
        }
    }

    interface OnSeekListener {
        fun onDragEnd(position: Long)

        fun onDrag()
    }

}
