package com.shiyuan.fastclaw.common.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 为 IME 动画多布局少量额外 item，防止列表子项整体位移时边缘短暂露白。
 * [stackFromEnd] 只参与动画前后的静态布局，不参与动画逐帧计算。
 */
class ImeAnimationLinearLayoutManager(context: Context) : LinearLayoutManager(context) {

    private var imeAnimationExtraLayoutSpace = 0

    fun setImeAnimationExtraLayoutSpace(extraLayoutSpace: Int) {
        val normalizedSpace = extraLayoutSpace.coerceAtLeast(0)
        if (imeAnimationExtraLayoutSpace == normalizedSpace) return
        val needsLayout = normalizedSpace > imeAnimationExtraLayoutSpace
        imeAnimationExtraLayoutSpace = normalizedSpace
        // 增大预布局范围需要立即重排；缩小时保留既有缓存，避免动画末帧再触发一次布局。
        if (needsLayout) requestLayout()
    }

    override fun calculateExtraLayoutSpace(
        state: RecyclerView.State,
        extraLayoutSpace: IntArray,
    ) {
        super.calculateExtraLayoutSpace(state, extraLayoutSpace)
        extraLayoutSpace[0] = maxOf(extraLayoutSpace[0], imeAnimationExtraLayoutSpace)
        extraLayoutSpace[1] = maxOf(extraLayoutSpace[1], imeAnimationExtraLayoutSpace)
    }
}

/**
 * IME 开始前记录首帧位置，先完成一次终态布局，再仅通过 translation/子项 offset 跟随动画。
 * 这样可避免 stackFromEnd 在每次 padding 变化后反复选锚点和修正尾部间隙。
 */
class ImeInsetsAnimationCoordinator(
    private val rootView: View,
    private val inputView: View,
    private val recyclerView: RecyclerView,
    private val imeHiddenView: View? = null,
    private val imeBackgroundView: View? = null,
    private val imeHiddenDecorationView: View? = null,
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
    private val initialImeHiddenViewSpace =
        initialImeHiddenViewHeight + initialImeHiddenViewBottomMargin

    private var imeAnimating = false
    private var finalLayoutPrepared = false
    private var hasAppliedInsets = false
    private var keepListAtBottom = false
    private var currentImeHeight = 0
    private var targetImeHeight = 0
    private var animationStartImeHeight = 0
    private var animationStartOffset = closedOffsetPx
    private var animationStartInputTop = 0
    private var animationStartRecyclerPaddingBottom = initialRecyclerPaddingBottom
    private var animationStartOpenFraction = 0f
    private var animationStartListAnchorEnd = 0
    private var animationListAnchorPosition = RecyclerView.NO_POSITION
    private var animationStartListAnchorTop = 0
    private var animationInputTranslationY = 0f
    private var listStartVisualOffset = 0
    private var currentListVisualOffset = 0
    private var lastAppliedOffset = closedOffsetPx
    private var lastAppliedActionSpace = initialImeHiddenViewSpace
    private var imeHiddenViewOpenFraction = 0f
    private var postImeActionAnimator: ValueAnimator? = null
    private var postImeActionRevealStarted = false
    private var postImeActionDurationMillis = DEFAULT_ACTION_REVEAL_DURATION_MILLIS
    private var postImeActionInterpolator: TimeInterpolator? = null
    private var listAnchorPosition = RecyclerView.NO_POSITION
    private var listAnchorEndGap = 0
    private var listAnchorDecoratedHeight = 0
    private val listAnchorBounds = Rect()
    private val backgroundClipBounds = Rect()
    private var lastImeVisible: Boolean? = null

    private val inputLayoutChangeListener =
        View.OnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top == oldBottom - oldTop) return@OnLayoutChangeListener
            if (imeAnimating) return@OnLayoutChangeListener
            keepListAtBottom = captureListAnchor()
            applyFinalGeometry(lastAppliedOffset, lastAppliedActionSpace)
        }

    private val backgroundLayoutChangeListener =
        View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            applyBackgroundVisual(imeHiddenViewOpenFraction, inputView.translationY)
        }

    fun attach() {
        inputView.addOnLayoutChangeListener(inputLayoutChangeListener)
        imeBackgroundView?.addOnLayoutChangeListener(backgroundLayoutChangeListener)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val appliedImeHeight = insets.imeHeightAboveNavigationBars()
            targetImeHeight = appliedImeHeight
            if (imeAnimating) {
                if (!finalLayoutPrepared) {
                    prepareAnimationFinalLayout(appliedImeHeight)
                }
            } else if (!hasAppliedInsets || currentImeHeight != appliedImeHeight) {
                keepListAtBottom = captureListAnchor()
                val openFraction = if (appliedImeHeight > 0) 1f else 0f
                applyImeHiddenViewOpenFraction(openFraction)
                applyFinalGeometry(
                    offsetForSettledIme(appliedImeHeight),
                    imeHiddenViewSpace(openFraction),
                )
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
                    cancelPostImeActionReveal()
                    postImeActionRevealStarted = false
                    imeAnimating = true
                    // IME 终态布局会先于首个进度帧落位，提前通知可避免附属按钮先跳到键盘上方。
                    if (currentImeHeight <= 0) dispatchImeVisibility(1)
                    finalLayoutPrepared = false
                    animationStartImeHeight = currentImeHeight
                    animationStartOffset = lastAppliedOffset
                    animationStartInputTop = inputView.top
                    animationStartRecyclerPaddingBottom = recyclerView.paddingBottom
                    animationStartOpenFraction = imeHiddenViewOpenFraction
                    if (animation.durationMillis > 0L) {
                        postImeActionDurationMillis = animation.durationMillis.coerceIn(
                            MIN_ACTION_REVEAL_DURATION_MILLIS,
                            MAX_ACTION_REVEAL_DURATION_MILLIS,
                        )
                    }
                    postImeActionInterpolator = animation.interpolator
                    keepListAtBottom = captureListAnchor()
                    animationStartListAnchorEnd = if (keepListAtBottom) {
                        listAnchorBounds.bottom
                    } else {
                        captureFreeListAnchor()
                        0
                    }
                    recyclerView.stopScroll()
                }

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat,
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    if (!animation.affectsIme()) return bounds
                    restoreAnimationStartPositions()
                    return bounds
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat {
                    if (runningAnimations.none { it.affectsIme() }) return insets
                    currentImeHeight = insets.imeHeightAboveNavigationBars()
                    val progress = calculateImeProgress(currentImeHeight)
                    applyAnimationProgress(progress)
                    if (currentImeHeight > 0) dispatchImeVisibility(currentImeHeight)
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (!animation.affectsIme()) return
                    imeAnimating = false
                    finalLayoutPrepared = false
                    currentImeHeight = targetImeHeight
                    clearVisualOffsets()
                    setExtraLayoutSpace(0)
                    if (targetImeHeight > 0) {
                        applyImeHiddenViewOpenFraction(1f)
                        dispatchImeVisibility(targetImeHeight)
                    } else if (!postImeActionRevealStarted) {
                        postImeActionRevealStarted = true
                        startPostImeActionReveal()
                    }
                }
            },
        )
        ViewCompat.requestApplyInsets(rootView)
    }

    fun release() {
        cancelPostImeActionReveal()
        clearVisualOffsets()
        setExtraLayoutSpace(0)
        inputView.removeOnLayoutChangeListener(inputLayoutChangeListener)
        imeBackgroundView?.removeOnLayoutChangeListener(backgroundLayoutChangeListener)
        restoreImeHiddenView()
        ViewCompat.setWindowInsetsAnimationCallback(rootView, null)
        ViewCompat.setOnApplyWindowInsetsListener(rootView, null)
    }

    /** 动画期间只在终态 Insets 到达时执行一次真实布局。 */
    private fun prepareAnimationFinalLayout(imeHeight: Int) {
        val targetOpenFraction = if (imeHeight > 0) 1f else 0f
        val targetOffset = offsetForSettledIme(imeHeight)
        val targetActionSpace = imeHiddenViewSpace(targetOpenFraction)
        val targetRecyclerPaddingBottom = recyclerBottomPadding(targetOffset, targetActionSpace)
        setExtraLayoutSpace(
            abs(targetRecyclerPaddingBottom - animationStartRecyclerPaddingBottom),
        )
        applyFinalGeometry(targetOffset, targetActionSpace)
        finalLayoutPrepared = true
        hasAppliedInsets = true
    }

    /**
     * onApplyWindowInsets 已经完成终态布局；onStart 在绘制前把输入区和列表还原到动画首帧。
     */
    private fun restoreAnimationStartPositions() {
        val fallbackInputTranslation = (lastAppliedOffset - animationStartOffset).toFloat()
        val measuredInputTranslation = (animationStartInputTop - inputView.top).toFloat()
        animationInputTranslationY = if (
            measuredInputTranslation != 0f || fallbackInputTranslation == 0f
        ) {
            measuredInputTranslation
        } else {
            fallbackInputTranslation
        }
        applyInputTranslation(animationInputTranslationY)

        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val anchorPosition = if (keepListAtBottom) {
            listAnchorPosition
        } else {
            animationListAnchorPosition
        }
        val anchorView = if (anchorPosition != RecyclerView.NO_POSITION) {
            layoutManager.findViewByPosition(anchorPosition)
        } else {
            null
        }
        val measuredListOffset = anchorView?.let { view ->
            layoutManager.getDecoratedBoundsWithMargins(view, listAnchorBounds)
            if (keepListAtBottom) {
                animationStartListAnchorEnd - listAnchorBounds.bottom
            } else {
                animationStartListAnchorTop - listAnchorBounds.top
            }
        } ?: 0
        val fallbackListOffset =
            recyclerView.paddingBottom - animationStartRecyclerPaddingBottom
        listStartVisualOffset = if (
            anchorView != null && (measuredListOffset != 0 || fallbackListOffset == 0)
        ) {
            measuredListOffset
        } else {
            fallbackListOffset
        }
        setListVisualOffset(listStartVisualOffset)
    }

    private fun applyAnimationProgress(progress: Float) {
        val remainingFraction = 1f - progress
        applyInputTranslation(animationInputTranslationY * remainingFraction)
        setListVisualOffset((listStartVisualOffset * remainingFraction).roundToInt())

        if (targetImeHeight > 0) {
            applyImeHiddenViewOpenFraction(
                animationStartOpenFraction +
                    (1f - animationStartOpenFraction) * progress,
            )
        } else if (!postImeActionRevealStarted) {
            applyImeHiddenViewOpenFraction(1f)
            if (abs(inputView.translationY) <= 1f) {
                postImeActionRevealStarted = true
                startPostImeActionReveal()
            }
        }
    }

    private fun calculateImeProgress(imeHeight: Int): Float {
        val heightRange = targetImeHeight - animationStartImeHeight
        if (heightRange == 0) return 1f
        return ((imeHeight - animationStartImeHeight).toFloat() / heightRange)
            .coerceIn(0f, 1f)
    }

    private fun offsetForSettledIme(imeHeight: Int): Int {
        if (imeHeight <= 0) return closedOffsetPx
        // 本页根布局已为主导航预留 52dp，IME 展开时需扣掉这份既有占位。
        return (imeHeight + visibleGapPx - rootView.paddingBottom)
            .coerceAtLeast(closedOffsetPx)
    }

    /** 更新终态几何；动画逐帧回调不会调用本方法。 */
    private fun applyFinalGeometry(offset: Int, actionSpace: Int) {
        val targetBottomPadding = recyclerBottomPadding(offset, actionSpace)
        prepareListAnchorForFinalLayout(targetBottomPadding)
        if (recyclerView.paddingBottom != targetBottomPadding) {
            recyclerView.setPadding(
                initialRecyclerPaddingLeft,
                initialRecyclerPaddingTop,
                initialRecyclerPaddingRight,
                targetBottomPadding,
            )
        }
        val layoutParams = inputView.layoutParams as? MarginLayoutParams
        if (layoutParams != null && layoutParams.bottomMargin != offset) {
            layoutParams.bottomMargin = offset
            inputView.requestLayout()
        }
        lastAppliedOffset = offset
        lastAppliedActionSpace = actionSpace
    }

    private fun recyclerBottomPadding(offset: Int, actionSpace: Int): Int {
        return initialRecyclerPaddingBottom + inputView.height + offset + actionSpace
    }

    /** 记录动画前尾项相对可见底边的间距。 */
    private fun captureListAnchor(): Boolean {
        if (!RecyclerViewScrollHelper.isScrollBottom(recyclerView, 1)) return false
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return false
        val lastPosition = layoutManager.itemCount - 1
        if (lastPosition < 0) return false
        val lastView = layoutManager.findViewByPosition(lastPosition) ?: return false
        layoutManager.getDecoratedBoundsWithMargins(lastView, listAnchorBounds)
        listAnchorPosition = lastPosition
        listAnchorEndGap = recyclerView.height -
            recyclerView.paddingBottom - listAnchorBounds.bottom
        listAnchorDecoratedHeight = listAnchorBounds.height()
        return true
    }

    /** 非贴底时记录首个可见 item，供终态布局完成后还原动画首帧。 */
    private fun captureFreeListAnchor() {
        animationListAnchorPosition = RecyclerView.NO_POSITION
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstPosition = layoutManager.findFirstVisibleItemPosition()
        if (firstPosition == RecyclerView.NO_POSITION) return
        val firstView = layoutManager.findViewByPosition(firstPosition) ?: return
        layoutManager.getDecoratedBoundsWithMargins(firstView, listAnchorBounds)
        animationListAnchorPosition = firstPosition
        animationStartListAnchorTop = listAnchorBounds.top
    }

    /** 给终态布局写入一次尾项锚点，之后不再逐帧触发 LinearLayoutManager 布局。 */
    private fun prepareListAnchorForFinalLayout(targetBottomPadding: Int) {
        if (!keepListAtBottom || listAnchorPosition == RecyclerView.NO_POSITION) return
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val desiredEnd = recyclerView.height - targetBottomPadding - listAnchorEndGap
        val offsetFromStart = desiredEnd - listAnchorDecoratedHeight - recyclerView.paddingTop
        layoutManager.scrollToPositionWithOffset(listAnchorPosition, offsetFromStart)
    }

    private fun applyInputTranslation(translationY: Float) {
        inputView.translationY = translationY
        imeHiddenView?.translationY = translationY
        applyBackgroundVisual(imeHiddenViewOpenFraction, translationY)
    }

    /** 背景不参与淡入淡出，只裁掉 action 已收起的高度，并让顶部渐变贴着可见背景移动。 */
    private fun applyBackgroundVisual(openFraction: Float, inputTranslationY: Float) {
        val hiddenActionSpace =
            (initialImeHiddenViewSpace * openFraction.coerceIn(0f, 1f)).roundToInt()
        imeBackgroundView?.let { view ->
            view.translationY = inputTranslationY
            if (hiddenActionSpace == 0 || view.width == 0 || view.height == 0) {
                view.clipBounds = null
            } else {
                backgroundClipBounds.set(
                    0,
                    hiddenActionSpace.coerceAtMost(view.height),
                    view.width,
                    view.height,
                )
                view.clipBounds = backgroundClipBounds
            }
        }
        // ConstraintLayout 不会把目标 View 的 translation 传给依赖它的兄弟 View。
        imeHiddenDecorationView?.translationY = inputTranslationY + hiddenActionSpace
    }

    private fun setListVisualOffset(targetOffset: Int) {
        val offsetDelta = targetOffset - currentListVisualOffset
        if (offsetDelta == 0) return
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        layoutManager.offsetChildrenVertical(offsetDelta)
        currentListVisualOffset = targetOffset
    }

    private fun clearVisualOffsets() {
        applyInputTranslation(0f)
        setListVisualOffset(0)
        animationInputTranslationY = 0f
        listStartVisualOffset = 0
        animationListAnchorPosition = RecyclerView.NO_POSITION
    }

    private fun setExtraLayoutSpace(extraLayoutSpace: Int) {
        (recyclerView.layoutManager as? ImeAnimationLinearLayoutManager)
            ?.setImeAnimationExtraLayoutSpace(extraLayoutSpace)
    }

    private fun startPostImeActionReveal() {
        if (imeHiddenView == null) {
            dispatchImeVisibility(0)
            return
        }
        val animator = ValueAnimator.ofFloat(imeHiddenViewOpenFraction, 0f).apply {
            duration = postImeActionDurationMillis
            postImeActionInterpolator?.let { interpolator = it }
            addUpdateListener { valueAnimator ->
                applyImeHiddenViewOpenFraction(valueAnimator.animatedValue as Float)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (postImeActionAnimator !== animation) return
                    postImeActionAnimator = null
                    applyImeHiddenViewOpenFraction(0f)
                    // Action 区恢复完成后再允许附属按钮展示，保持其历史稳定间距。
                    dispatchImeVisibility(0)
                }
            })
        }
        postImeActionAnimator = animator
        animator.start()
    }

    private fun cancelPostImeActionReveal() {
        val animator = postImeActionAnimator ?: return
        postImeActionAnimator = null
        animator.cancel()
    }

    /** 快捷操作区保留固定布局尺寸，只改变视觉属性，不再推动列表重新布局。 */
    private fun applyImeHiddenViewOpenFraction(openFraction: Float) {
        val view = imeHiddenView ?: return
        val normalizedOpenFraction = openFraction.coerceIn(0f, 1f)
        val visibleFraction = 1f - normalizedOpenFraction
        if (view.visibility != View.VISIBLE) view.visibility = View.VISIBLE
        if (view.height > 0) view.pivotY = (view.height - view.paddingBottom).toFloat()
        view.alpha = visibleFraction
        view.scaleY = visibleFraction
        imeHiddenViewOpenFraction = normalizedOpenFraction
        applyBackgroundVisual(normalizedOpenFraction, inputView.translationY)
    }

    private fun imeHiddenViewSpace(openFraction: Float): Int {
        val visibleFraction = 1f - openFraction.coerceIn(0f, 1f)
        return (initialImeHiddenViewSpace * visibleFraction).roundToInt()
    }

    private fun restoreImeHiddenView() {
        imeHiddenView?.let { view ->
            view.alpha = 1f
            view.scaleY = 1f
            view.translationY = 0f
            view.visibility = View.VISIBLE
        }
        imeBackgroundView?.let { view ->
            view.clipBounds = null
            view.translationY = 0f
        }
        imeHiddenDecorationView?.translationY = 0f
        imeHiddenViewOpenFraction = 0f
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

    private fun WindowInsetsCompat.imeHeightAboveNavigationBars(): Int {
        val imeBottom = getInsets(WindowInsetsCompat.Type.ime()).bottom
        val navigationBottom = getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        return (imeBottom - navigationBottom).coerceAtLeast(0)
    }

    private companion object {
        const val DEFAULT_ACTION_REVEAL_DURATION_MILLIS = 200L
        const val MIN_ACTION_REVEAL_DURATION_MILLIS = 160L
        const val MAX_ACTION_REVEAL_DURATION_MILLIS = 300L
    }
}
