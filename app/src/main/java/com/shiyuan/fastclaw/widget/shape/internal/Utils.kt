package com.shiyuan.fastclaw.widget.shape.internal

import android.graphics.drawable.GradientDrawable


fun getColorOrientation(@ColorOrientation orientation: Int): GradientDrawable.Orientation {
    when (orientation) {
        TOP_BOTTOM -> {
            return GradientDrawable.Orientation.TOP_BOTTOM
        }
        TR_BL -> {
            return GradientDrawable.Orientation.TR_BL
        }
        RIGHT_LEFT -> {
            return GradientDrawable.Orientation.RIGHT_LEFT
        }
        BR_TL -> {
            return GradientDrawable.Orientation.BR_TL
        }
        BOTTOM_TOP -> {
            return GradientDrawable.Orientation.BOTTOM_TOP
        }
        BL_TR -> {
            return GradientDrawable.Orientation.BL_TR
        }
        LEFT_RIGHT -> {
            return GradientDrawable.Orientation.LEFT_RIGHT
        }
        TL_BR -> {
            return GradientDrawable.Orientation.TL_BR
        }
        else -> {
            return GradientDrawable.Orientation.LEFT_RIGHT
        }
    }
}