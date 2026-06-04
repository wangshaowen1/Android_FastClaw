package com.shiyuan.fastclaw.widget.shape.internal.superline

import android.graphics.Color
import com.shiyuan.fastclaw.widget.shape.internal.Constant.VALUE_NULL


class SuperLineDefaultStore {
    /**
     * 直线方向
     */
    var lineOrientation = VALUE_NULL
    /**
     * 直线颜色
     */
    var lineColor = Color.GRAY
    /**
     * 直线渐变开始颜色
     */
    var lineStartColor = VALUE_NULL
    /**
     * 直线渐变结束颜色
     */
    var lineEndColor = VALUE_NULL
    /**
     * 虚线间隙宽度
     */
    var lineDashGapWidth = VALUE_NULL
    /**
     * 虚线宽度
     */
    var lineDashWidth = VALUE_NULL
    /**
     * 虚线颜色
     */
    var lineDashGapColor = VALUE_NULL
}