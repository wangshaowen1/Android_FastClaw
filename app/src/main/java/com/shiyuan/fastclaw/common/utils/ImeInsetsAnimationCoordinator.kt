package com.shiyuan.fastclaw.common.utils

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * 使用同一份 IME 动画进度同步底部输入区、消息列表与快捷操作区，避免二次动画造成跳动。
 */
class ImeInsetsAnimationCoordinator(
    private val rootView: View,
    private val inputView: View,
    private val recyclerView: RecyclerView,
    private val imeHiddenView: View? = null,
    private val closedOffsetPx: Int,
    private val visibleGapPx: Int,
    private val onImeVisibilityChanged: (Boolean) -> Unit,
) {

    private val initialRecyclerPaddingLeft = recyclerView.paddingLeft
    private val initialRecyclerPaddingTop = recyclerView.paddingTop
    private val initialRecyclerPaddingRight = recyclerView.paddingRight
    private val initialRecyclerPaddingBottom = recyclerView.paddingBottom
    private val initialImeHiddenViewHeight = imeHiddenView?.layoutParams?.height ?: 0
    private val initialImeHiddenViewBottomMargin =
        (imeHiddenView?.layoutParams as? MarginLayoutParams)?.bottomMargin ?: 0

    private var imeAnimating = false
    private var hasAppliedInsets = false
    private var keepListAtBottom = false
    private var currentImeHeight = 0
    private var targetImeHeight = 0
    private var lastAppliedOffset = closedOffsetPx
    private var imeAnimationStartOpenFraction = 0f
    private var imeHiddenViewOpenFraction = 0f
    private var listAnchorPosition = RecyclerView.NO_POSITION
    private var listAnchorEndGap = 0
    private val listAnchorBounds = Rect()
    private var laidOutOffset =
        (inputView.layoutParams as? MarginLayoutParams)?.bottomMargin ?: 0
    private var lastImeVisible: Boolean? = null

    private val inputLayoutChangeListener =
        View.OnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            laidOutOffset =
                (inputView.layoutParams as? MarginLayoutParams)?.bottomMargin ?: laidOutOffset
            syncInputTranslation()
            if (bottom - top == oldBottom - oldTop) return@OnLayoutChangeListener
            keepListAtBottom = captureListAnchor()
            applyOffset(lastAppliedOffset)
        }

    /** 注册 Insets 与输入区尺寸监听，页面销毁时必须调用 [release]。 */
    fun attach() {
        inputView.addOnLayoutChangeListener(inputLayoutChangeListener)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val appliedImeHeight = insets.imeHeightAboveNavigationBars()
            targetImeHeight = appliedImeHeight
            if (!imeAnimating && (!hasAppliedInsets || currentImeHeight != appliedImeHeight)) {
                keepListAtBottom = captureListAnchor()
                applyImeHiddenViewOpenFraction(if (appliedImeHeight > 0) 1f else 0f)
                applyOffset(offsetForSettledIme(appliedImeHeight))
                currentImeHeight = appliedImeHeight
                hasAppliedInsets = true
                dispatchImeVisibility(appliedImeHeight)
            }
            insets
        }
        ViewCompat.setWindowInsetsAnimationCallback(
            rootView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    if (!animation.affectsIme()) return
                    imeAnimating = true
                    imeAnimationStartOpenFraction = imeHiddenViewOpenFraction
                    keepListAtBottom = captureListAnchor()
                    recyclerView.stopScroll()
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat {
                    val imeAnimation = runningAnimations.firstOrNull { it.affectsIme() }
                        ?: return insets
                    currentImeHeight = insets.imeHeightAboveNavigationBars()
                    val targetOpenFraction = if (targetImeHeight > 0) 1f else 0f
                    val animationFraction = imeAnimation.interpolatedFraction.coerceIn(0f, 1f)
                    applyImeHiddenViewOpenFraction(
                        imeAnimationStartOpenFraction +
                            (targetOpenFraction - imeAnimationStartOpenFraction) * animationFraction
                    )
                    applyOffset(offsetForSettledIme(currentImeHeight))
                    // 部分系统会在键盘表面动画结束前先把 IME inset 降为 0，隐藏态延后到 onEnd。
                    if (currentImeHeight > 0) dispatchImeVisibility(currentImeHeight)
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (!animation.affectsIme()) return
                    imeAnimating = false
                    currentImeHeight = targetImeHeight
                    applyImeHiddenViewOpenFraction(if (targetImeHeight > 0) 1f else 0f)
                    applyOffset(offsetForSettledIme(targetImeHeight))
                    dispatchImeVisibility(currentImeHeight)
                }
            }
        )
        ViewCompat.requestApplyInsets(rootView)
    }

    /** 移除页面级监听，避免 Fragment 重建后旧 View 继续接收回调。 */
    fun release() {
        inputView.removeOnLayoutChangeListener(inputLayoutChangeListener)
        inputView.translationY = 0f
        restoreImeHiddenView()
        ViewCompat.setWindowInsetsAnimationCallback(rootView, null)
        ViewCompat.setOnApplyWindowInsetsListener(rootView, null)
    }

    /** 根布局已有底部导航占位，IME 展开时只补足到键盘顶部的剩余距离。 */
    private fun offsetForSettledIme(imeHeight: Int): Int {
        if (imeHeight <= 0) return closedOffsetPx
        return (imeHeight + visibleGapPx - rootView.paddingBottom).coerceAtLeast(closedOffsetPx)
    }

    /** 同帧更新输入区位置与列表留白，列表原本贴底时同步保持尾项间距。 */
    private fun applyOffset(offset: Int) {
        val layoutParams = inputView.layoutParams as? MarginLayoutParams ?: return
        if (layoutParams.bottomMargin != offset) {
            layoutParams.bottomMargin = offset
            inputView.requestLayout()
        }
        lastAppliedOffset = offset
        // margin 会在下一次布局才落位，先用 translation 补到当前 IME 帧，避免收起时慢一拍。
        syncInputTranslation()

        val targetBottomPadding = initialRecyclerPaddingBottom + inputView.height + offset
        if (targetBottomPadding != recyclerView.paddingBottom) {
            recyclerView.setPadding(
                initialRecyclerPaddingLeft,
                initialRecyclerPaddingTop,
                initialRecyclerPaddingRight,
                targetBottomPadding,
            )
        }
        if (keepListAtBottom) alignListAnchor(targetBottomPadding)
    }

    /** 快捷操作区随键盘进度同步收缩或展开，不额外启动独立 View 动画。 */
    private fun applyImeHiddenViewOpenFraction(openFraction: Float) {
        val view = imeHiddenView ?: return
        val normalizedOpenFraction = openFraction.coerceIn(0f, 1f)
        val visibleFraction = 1f - normalizedOpenFraction
        if (visibleFraction > 0f && view.visibility != View.VISIBLE) {
            view.visibility = View.VISIBLE
        }
        view.alpha = visibleFraction

        val layoutParams = view.layoutParams as? MarginLayoutParams
        if (layoutParams != null) {
            // ConstraintLayout 中 height=0 表示 MATCH_CONSTRAINT，隐藏终帧会重新撑回内容高度。
            // 动画过程最低保留 1px，完全隐藏时交给 GONE 真正移除布局占位。
            val targetHeight = maxOf(
                1,
                (initialImeHiddenViewHeight * visibleFraction).roundToInt(),
            )
            val targetBottomMargin =
                (initialImeHiddenViewBottomMargin * visibleFraction).roundToInt()
            if (layoutParams.height != targetHeight ||
                layoutParams.bottomMargin != targetBottomMargin
            ) {
                layoutParams.height = targetHeight
                layoutParams.bottomMargin = targetBottomMargin
                view.requestLayout()
            }
        }

        if (visibleFraction == 0f) view.visibility = View.GONE
        imeHiddenViewOpenFraction = normalizedOpenFraction
    }

    private fun restoreImeHiddenView() {
        val view = imeHiddenView ?: return
        val layoutParams = view.layoutParams as? MarginLayoutParams
        if (layoutParams != null) {
            layoutParams.height = initialImeHiddenViewHeight
            layoutParams.bottomMargin = initialImeHiddenViewBottomMargin
            view.requestLayout()
        }
        view.alpha = 1f
        view.visibility = View.VISIBLE
        imeHiddenViewOpenFraction = 0f
    }

    /** 记录尾项当前的可见间距，动画中始终保持这份间距不变。 */
    private fun captureListAnchor(): Boolean {
        if (!RecyclerViewScrollHelper.isLastItemVisible(recyclerView)) return false
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return false
        val anchorPosition = (recyclerView.adapter?.itemCount ?: 0) - 1
        if (anchorPosition < 0) return false
        val anchorView = layoutManager.findViewByPosition(anchorPosition) ?: return false
        layoutManager.getDecoratedBoundsWithMargins(anchorView, listAnchorBounds)
        listAnchorPosition = anchorPosition
        listAnchorEndGap = recyclerView.height - recyclerView.paddingBottom - listAnchorBounds.bottom
        return true
    }

    /**
     * setPadding 会触发 stackFromEnd 的再次布局，不能再叠加 scrollBy。
     * 先把当前子项移到本帧目标位置，再给下一次布局写入同一个锚点，避免首帧抢跑或下一帧回弹。
     */
    private fun alignListAnchor(targetBottomPadding: Int) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val anchorView = layoutManager.findViewByPosition(listAnchorPosition) ?: return
        layoutManager.getDecoratedBoundsWithMargins(anchorView, listAnchorBounds)
        val decoratedHeight = listAnchorBounds.height()
        val desiredEnd = recyclerView.height - targetBottomPadding - listAnchorEndGap
        val childOffset = desiredEnd - listAnchorBounds.bottom
        if (childOffset != 0) layoutManager.offsetChildrenVertical(childOffset)

        val offsetFromStart = desiredEnd - decoratedHeight - recyclerView.paddingTop
        layoutManager.scrollToPositionWithOffset(listAnchorPosition, offsetFromStart)
    }

    private fun syncInputTranslation() {
        inputView.translationY = (laidOutOffset - lastAppliedOffset).toFloat()
    }

    private fun dispatchImeVisibility(imeHeight: Int) {
        val visible = imeHeight > 0
        if (lastImeVisible == visible) return
        lastImeVisible = visible
        onImeVisibilityChanged(visible)
    }

    private fun WindowInsetsAnimationCompat.affectsIme(): Boolean {
        return typeMask and WindowInsetsCompat.Type.ime() != 0
    }

    /** 页面内容区已避让系统导航栏，IME 位移需扣除导航栏高度。 */
    private fun WindowInsetsCompat.imeHeightAboveNavigationBars(): Int {
        val imeBottom = getInsets(WindowInsetsCompat.Type.ime()).bottom
        val navigationBottom = getInsetsIgnoringVisibility(
            WindowInsetsCompat.Type.navigationBars()
        ).bottom
        return (imeBottom - navigationBottom).coerceAtLeast(0)
    }
}
