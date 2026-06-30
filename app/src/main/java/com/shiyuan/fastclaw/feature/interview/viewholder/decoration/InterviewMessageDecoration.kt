package com.shiyuan.fastclaw.feature.interview.viewholder.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nhsd.basic.ext.dp

/**
 * 面试消息列表顶部间距：只给第一条消息增加顶部留白。
 */
class InterviewMessageDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = 5.dp
        }
    }
}