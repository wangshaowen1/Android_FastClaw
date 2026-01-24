package com.kgmedia.fastfilm.common.ext

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.children
import com.blankj.utilcode.util.ActivityUtils
import com.kgmedia.fastfilm.R
import com.nhsd.basic.ext.dp


fun View.shake() {
    (getTag(R.id.shakeAnim) as? Animator)?.cancel()
    val dp5 = 5.dp.toFloat()
    ObjectAnimator.ofFloat(this, "translationX", 0f, -dp5, 0f, dp5, 0f)
        .apply {
            duration = 300
            repeatCount = 1
            repeatMode = ValueAnimator.RESTART
            start()
        }.let {
            setTag(R.id.shakeAnim, it)
        }
}

fun <LayoutParams : ViewGroup.LayoutParams> View.updateLayoutParams(transform: (LayoutParams) -> Unit) {
    (layoutParams as? LayoutParams)?.let {
        transform(it)
        layoutParams = it
    }
}

// EditText扩展
fun EditText.setProhibitGetFocus(isProhibit: Boolean) {
    if (isProhibit) {
        isCursorVisible = false
        isFocusable = false
        isFocusableInTouchMode = false
        setOnClickListener(null)
    } else {
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
        isCursorVisible = true
    }
}

fun EditText.trimText(): String = text.toString().trim()


// ========================================================================================
// View 动画扩展
// ========================================================================================

fun View.animateScaleEnter(duration: Long = 300, listener: Animator.AnimatorListener? = null) {
    val p1 = PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
    val p2 = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f)
    val p3 = PropertyValuesHolder.ofFloat("scaleY", 0f, 1f)
    val p4 = PropertyValuesHolder.ofObject("backgroundColor", ArgbEvaluator(), Color.TRANSPARENT, Color.BLACK)

    ObjectAnimator.ofPropertyValuesHolder(this, p1, p2, p3, p4).apply {
        this.duration = duration
        if (listener != null) addListener(listener)
        start()
    }
}

fun View.animateScaleExit(duration: Long = 300, listener: Animator.AnimatorListener? = null) {
    val p1 = PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
    val p2 = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f)
    val p3 = PropertyValuesHolder.ofFloat("scaleY", 1f, 0f)
    val p4 = PropertyValuesHolder.ofObject("backgroundColor", ArgbEvaluator(), Color.BLACK, Color.TRANSPARENT)

    ObjectAnimator.ofPropertyValuesHolder(this, p1, p2, p3, p4).apply {
        this.duration = duration
        if (listener != null) addListener(listener)
        start()
    }
}


fun ViewGroup.getChildByType(vararg type: Class<out View>): View? {
    for (child in children.toList()) {
        if (type.contains(child.javaClass)) {
            return child
        }
        if (child is ViewGroup) {
            return child.getChildByType(*type)
        }
    }
    return null
}

fun View.getActivity(): Activity = ActivityUtils.getActivityByContext(context) ?: ActivityUtils.getTopActivity()