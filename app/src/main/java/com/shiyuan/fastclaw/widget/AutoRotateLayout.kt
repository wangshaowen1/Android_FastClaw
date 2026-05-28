package com.shiyuan.fastclaw.widget

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.shiyuan.fastclaw.R

/**
 * 自动旋转布局
 * - 可见性跟随：attach 到窗口且可见时自动开始旋转，detach 或不可见时停止
 * - 支持手动控制：通过 autoRotate 属性或 setAutoRotate()
 */
class AutoRotateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * 是否启用自动旋转，true 时跟随可见性启停，false 时停止旋转
     */
    var autoRotate = false
        set(value) {
            if (field != value) {
                field = value
                if (!value) {
                    stopRotate()
                } else if (isAttachedToWindow && visibility == VISIBLE) {
                    startRotate()
                }
            }
        }

    private var duration = 5000L
    private var rotateAnimator: Animator? = null

    private val attachListener = object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            if (autoRotate && visibility == VISIBLE) {
                startRotate()
            }
        }

        override fun onViewDetachedFromWindow(v: View) {
            stopRotate()
        }
    }

    init {
        addOnAttachStateChangeListener(attachListener)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AutoRotateLayout)
        duration = typedArray.getInt(R.styleable.AutoRotateLayout_duration, 5000).toLong()
        autoRotate = typedArray.getBoolean(R.styleable.AutoRotateLayout_autoRotate, false)
        typedArray.recycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 初始化时如果 autoRotate=true 且可见，立即开始旋转
        if (autoRotate && visibility == VISIBLE) {
            startRotate()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRotate()
    }

    override fun setVisibility(visibility: Int) {
        val oldVisibility = getVisibility()
        super.setVisibility(visibility)
        if (oldVisibility != visibility) {
            when (visibility) {
                VISIBLE -> if (autoRotate && isAttachedToWindow) startRotate()
                else -> stopRotate()
            }
        }
    }

    private fun startRotate() {
        if (rotateAnimator != null) {
            return
        }
        rotateAnimator = ObjectAnimator.ofFloat(this, "rotation", 0f, 360f).also {
            rotateAnimator = it
            it.duration = duration
            it.repeatMode = ValueAnimator.RESTART
            it.repeatCount = ValueAnimator.INFINITE
            it.interpolator = LinearInterpolator()
            it.start()
        }
    }

    private fun stopRotate() {
        rotation = 0f
        rotateAnimator?.cancel()
        rotateAnimator = null
    }
}
