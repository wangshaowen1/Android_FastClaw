package com.shiyuan.fastclaw.feature.mine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 虾条明细列表背景
 * @createtime 2026年05月18日 17:43
 * @author TooWhiteT
 */

class MyShrimpListDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, context.resources.displayMetrics)
    private val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, context.resources.displayMetrics)

    // 偏移，预留padding空间
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position <= 0) return
        val itemCount = parent.adapter?.itemCount ?: return
        // 第二项 预留绘制背景空间
        if (position == 1) outRect.top = padding.toInt()
        // 最后一项 预留绘制背景空间
        if (position == itemCount - 1) outRect.bottom = padding.toInt()
    }

    // 绘制背景
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        var top = Float.MAX_VALUE
        var bottom = Float.MIN_VALUE
        val itemCount = parent.adapter?.itemCount ?: return
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            // position 0 是 header，跳过不绘制
            if (position <= 0) continue

            // 覆盖上下预留的区域，这样绘制背景不会超出控件测量范围
            top = if (position == 1) {
                child.top.toFloat() - padding
            } else {
                minOf(top, child.top.toFloat())
            }
            bottom = if (position == itemCount - 1) {
                child.bottom.toFloat() + padding
            } else {
                maxOf(bottom, child.bottom.toFloat())
            }
        }
        if (top == Float.MAX_VALUE) return
        c.drawRoundRect(0f, top, parent.width - 0f, bottom, radius, radius, paint)
    }
}