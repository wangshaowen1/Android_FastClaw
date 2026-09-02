package com.shiyuan.fastclaw.feature.interview

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.dp

/**
 * 面试消息列表间距：第一条顶部和最后一条底部默认留白 5dp。
 */
class InterviewMessageDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == 0) {
            outRect.top = 5.dp
        }
        if (position == state.itemCount - 1) {
            outRect.bottom = 5.dp
        }
    }
}
