package com.shiyuan.fastclaw.feature.resume.preference

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

internal data class StickyHeaderSection(
    val startPosition: Int,
    val title: String
)

internal class StickyHeaderDecoration(
    context: Context,
    @LayoutRes headerLayoutId: Int,
    @IdRes titleViewId: Int,
    private val spanCount: Int = 1,
    private val reserveHeaderSpace: Boolean = false
) : RecyclerView.ItemDecoration() {

    private val headerView = LayoutInflater.from(context).inflate(headerLayoutId, null, false).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    private val titleView: TextView = headerView.findViewById(titleViewId)
    private var sections: List<StickyHeaderSection> = emptyList()

    var headerHeight: Int = 0
        private set

    init {
        measureHeader(context.resources.displayMetrics.widthPixels, "")
    }

    fun submitSections(newSections: List<StickyHeaderSection>) {
        sections = newSections.toList()
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.set(0, 0, 0, 0)
        if (!reserveHeaderSpace) return

        val position = parent.getChildAdapterPosition(view)
        if (needsHeaderOffset(position)) {
            measureHeader(parent, sections.firstOrNull { it.startPosition == position }?.title.orEmpty())
            outRect.top = headerHeight
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!reserveHeaderSpace) return

        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            val section = sections.firstOrNull { it.startPosition == position } ?: continue
            measureHeader(parent, section.title)
            drawHeader(canvas, parent, child.top - headerHeight, section.title)
        }
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION) return

        val currentSection = sections.lastOrNull { it.startPosition <= firstVisible } ?: return
        val nextSection = sections.firstOrNull { it.startPosition > firstVisible }
        measureHeader(parent, currentSection.title)

        val baseTop = parent.paddingTop
        val nextHeaderTop = nextSection
            ?.let { parent.findViewHolderForAdapterPosition(it.startPosition)?.itemView }
            ?.let { if (reserveHeaderSpace) it.top - headerHeight else it.top }
        val top = nextHeaderTop
            ?.takeIf { it < baseTop + headerHeight }
            ?.let { it - headerHeight }
            ?: baseTop

        drawHeader(canvas, parent, top, currentSection.title)
    }

    private fun drawHeader(canvas: Canvas, parent: RecyclerView, top: Int, title: String) {
        measureHeader(parent, title)
        canvas.save()
        canvas.translate(parent.paddingLeft.toFloat(), top.toFloat())
        headerView.draw(canvas)
        canvas.restore()
    }

    private fun measureHeader(parent: RecyclerView, title: String) {
        val width = parent.width - parent.paddingLeft - parent.paddingRight
        if (width > 0) {
            measureHeader(width, title)
        }
    }

    private fun measureHeader(width: Int, title: String) {
        titleView.text = title
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        headerView.measure(widthSpec, heightSpec)
        headerHeight = headerView.measuredHeight
        headerView.layout(0, 0, width, headerHeight)
    }

    private fun needsHeaderOffset(position: Int): Boolean {
        if (position == RecyclerView.NO_POSITION) return false
        val section = sections.lastOrNull { it.startPosition <= position } ?: return false
        val nextSectionStart = sections.firstOrNull { it.startPosition > section.startPosition }?.startPosition
            ?: Int.MAX_VALUE
        return position < nextSectionStart && position < section.startPosition + spanCount
    }
}
