package com.shiyuan.fastclaw.feature.report.interview

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.shiyuan.fastclaw.R
import com.shiyuan.fastclaw.common.ext.showConfirmPrompt
import com.shiyuan.fastclaw.common.ext.startRefreshWithAdapter
import com.shiyuan.fastclaw.core.manager.AppEvent
import com.shiyuan.fastclaw.core.manager.EventManager
import com.shiyuan.fastclaw.databinding.ActivityInterviewReportBinding
import com.shiyuan.fastclaw.feature.report.data.ReportInfo
import com.shiyuan.fastclaw.feature.report.data.ReportStatus
import com.shiyuan.fastclaw.feature.web.SimpleBrowserActivity
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @Introduce: 历史报告列表页面
 */
class InterviewReportActivity :
    BaseViewModelActivity<ActivityInterviewReportBinding, InterviewReportViewModel>() {

    companion object {
        private const val REPORT_LIST_REFRESH_INTERVAL = 5000L

        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, InterviewReportActivity::class.java)
            activity.startActivity(intent)
        }
    }

    object ReportRefreshPolicy {

        fun shouldAutoRefresh(reports: List<ReportInfo>): Boolean {
            return reports.any { it.status == ReportStatus.IN_PROGRESS }
        }
    }

    private var reportListRefreshJob: Job? = null
    private var isReportListRefreshing = false
    private var swipeDeleteHelper: InterviewReportSwipeDeleteHelper? = null

    private val mListAdapter by lazy {
        InterviewReportAdapter {
            onReportItemClick(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObservers()
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(true)
            .navigationBarColor(R.color.color_F5F8FC)
            .transparentStatusBar()
            .init()
    }

    private fun initView() {
        binding.recyclerView.apply {
            layoutManager = InterviewReportViewModel.buildLayoutManager(context)
            addItemDecoration(InterviewReportViewModel.buildItemDecoration(context))
            adapter = mListAdapter
        }
        swipeDeleteHelper = InterviewReportSwipeDeleteHelper(
            recyclerView = binding.recyclerView,
            adapter = mListAdapter,
            onDelete = ::showDeleteConfirmPrompt
        ).also { it.attach() }
        binding.srl.startRefreshWithAdapter(
            mListAdapter,
            binding.lsv,
            onRefreshListener = {},
            addExtraLoadStateListener = {}
        )
        binding.lsv.setDefaultEmptyImage(R.drawable.ic_empty)
        binding.lsv.setNoDataPrompt(R.string.report_empty_tip.getResString())
        binding.lsv.setNoDataTipPrompt(R.string.report_empty_tip3.getResString())
    }

    private fun showDeleteConfirmPrompt(info: ReportInfo) {
        showConfirmPrompt(
            tip = R.string.report_delete_confirm_content,
            title = R.string.report_delete_confirm_title,
            confirmText = R.string.delete,
            cancelText = R.string.cancel,
            cancelable = false,
            cancel = {
                swipeDeleteHelper?.restoreOpenItem()
            },
            confirm = {
                mRealVM.hideReport(
                    reportId = info.id,
                    reportType = info.reportType,
                    onSuccess = {
                        swipeDeleteHelper?.clearOpenItem()
                        mListAdapter.refresh()
                    },
                    onError = {
                        swipeDeleteHelper?.restoreOpenItem()
                    },
                )
            }
        )
    }

    private fun onReportItemClick(info: ReportInfo) {
        val detailUrl = info.detailUrl?.takeIf { it.isNotBlank() } ?: return
        if (info.status != ReportStatus.OTHER || !info.canViewReport) return
        EventManager.onAppTrack(AppEvent.KLX_CAPP_INTERVIEWREPORT_REPORT_PAGE)
        SimpleBrowserActivity.launch(this, detailUrl,false,true)
    }

    private fun initObservers() {
        lifecycleScope.launch {
            mRealVM.reportListFlow.collectLatest { pagingData ->
                mListAdapter.submitData(pagingData)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    updateReportListAutoRefresh()
                    mListAdapter.loadStateFlow.collectLatest { loadStates ->
                        isReportListRefreshing = loadStates.refresh is LoadState.Loading
                        if (loadStates.refresh is LoadState.NotLoading) {
                            updateReportListAutoRefresh()
                        }
                    }
                } finally {
                    stopReportListAutoRefresh()
                }
            }
        }
    }

    private fun updateReportListAutoRefresh() {
        if (ReportRefreshPolicy.shouldAutoRefresh(mListAdapter.snapshot().items)) {
            startReportListAutoRefresh()
        } else {
            stopReportListAutoRefresh()
        }
    }

    private fun startReportListAutoRefresh() {
        if (reportListRefreshJob?.isActive == true) return
        reportListRefreshJob = lifecycleScope.launch {
            while (true) {
                delay(REPORT_LIST_REFRESH_INTERVAL)
                if (!isReportListRefreshing) {
                    mListAdapter.refresh()
                }
            }
        }
    }

    private fun stopReportListAutoRefresh() {
        reportListRefreshJob?.cancel()
        reportListRefreshJob = null
    }

    override fun onDestroy() {
        stopReportListAutoRefresh()
        swipeDeleteHelper?.detach()
        swipeDeleteHelper = null
        super.onDestroy()
    }

    private class InterviewReportSwipeDeleteHelper(
        private val recyclerView: RecyclerView,
        private val adapter: InterviewReportAdapter,
        private val onDelete: (ReportInfo) -> Unit
    ) : RecyclerView.SimpleOnItemTouchListener() {

        private val touchSlop = ViewConfiguration.get(recyclerView.context).scaledTouchSlop
        private val deleteView = LayoutInflater.from(recyclerView.context)
            .inflate(R.layout.view_interview_report_swipe_delete, recyclerView, false)
        private var deleteBitmap: Bitmap? = null
        private var deleteBitmapHeight = 0
        private var deleteWidth = 0
        private var downX = 0f
        private var downY = 0f
        private var startTranslationX = 0f
        private var isDragging = false
        private var isDeleteButtonPressed = false
        private var isPromptShowing = false
        private var activeHolder: RecyclerView.ViewHolder? = null
        private var openHolder: RecyclerView.ViewHolder? = null

        private val decoration = object : RecyclerView.ItemDecoration() {
            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                for (index in 0 until parent.childCount) {
                    val child = parent.getChildAt(index)
                    if (child.translationX < 0f) {
                        drawDeleteView(c, child)
                    }
                }
            }
        }

        fun attach() {
            recyclerView.addItemDecoration(decoration)
            recyclerView.addOnItemTouchListener(this)
        }

        fun detach() {
            recyclerView.removeOnItemTouchListener(this)
            recyclerView.removeItemDecoration(decoration)
            deleteBitmap?.recycle()
            deleteBitmap = null
        }

        override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (isPromptShowing) return false
                    downX = event.x
                    downY = event.y
                    isDragging = false
                    openHolder?.takeIf { isTouchingDeleteButton(event, it) }?.let { holder ->
                        activeHolder = holder
                        isDeleteButtonPressed = true
                        return true
                    }
                    isDeleteButtonPressed = false
                    activeHolder = rv.findChildViewUnder(event.x, event.y)?.let { child ->
                        openHolder?.takeIf { it.itemView != child }?.let { restoreOpenItem() }
                        rv.getChildViewHolder(child)
                    }
                    startTranslationX = activeHolder?.itemView?.translationX ?: 0f
                }
                MotionEvent.ACTION_MOVE -> {
                    val holder = activeHolder ?: return false
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (abs(dx) > touchSlop && abs(dx) > abs(dy) && (dx < 0f || holder.itemView.translationX < 0f)) {
                        isDragging = true
                        rv.parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    activeHolder = null
                    isDragging = false
                }
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
            if (isDeleteButtonPressed) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        if (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop) {
                            isDeleteButtonPressed = false
                            activeHolder = null
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        val holder = activeHolder
                        isDeleteButtonPressed = false
                        activeHolder = null
                        isPromptShowing = true
                        holder?.let { resolveReportInfo(it) }?.let(onDelete) ?: restoreOpenItem()
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        isDeleteButtonPressed = false
                        activeHolder = null
                    }
                }
                return
            }
            if (!isDragging) return
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> updateActiveItemTranslation(event.x)
                MotionEvent.ACTION_UP -> finishActiveSwipe()
                MotionEvent.ACTION_CANCEL -> restoreActiveItem()
            }
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit

        fun restoreOpenItem() {
            isPromptShowing = false
            openHolder?.let { animateItemTo(it.itemView, 0f) }
            openHolder = null
            activeHolder = null
            isDragging = false
            isDeleteButtonPressed = false
        }

        fun clearOpenItem() {
            isPromptShowing = false
            openHolder?.itemView?.translationX = 0f
            openHolder = null
            activeHolder = null
            isDragging = false
            isDeleteButtonPressed = false
            recyclerView.invalidateItemDecorations()
        }

        private fun updateActiveItemTranslation(currentX: Float) {
            val holder = activeHolder ?: return
            val maxOffset = getDeleteWidth(holder.itemView.height).toFloat()
            if (maxOffset <= 0f) return
            val targetTranslation = (startTranslationX + currentX - downX)
                .coerceIn(-maxOffset, 0f)
            holder.itemView.translationX = targetTranslation
            recyclerView.invalidateItemDecorations()
        }

        private fun finishActiveSwipe() {
            val holder = activeHolder ?: return
            val maxOffset = getDeleteWidth(holder.itemView.height).toFloat()
            if (maxOffset <= 0f) {
                restoreActiveItem()
                return
            }
            if (holder.itemView.translationX <= -maxOffset) {
                holder.itemView.translationX = -maxOffset
                openHolder = holder
                activeHolder = null
                isDragging = false
                recyclerView.invalidateItemDecorations()
            } else {
                restoreActiveItem()
            }
        }

        private fun restoreActiveItem() {
            activeHolder?.let { animateItemTo(it.itemView, 0f) }
            activeHolder = null
            isDragging = false
        }

        private fun animateItemTo(itemView: View, targetTranslationX: Float) {
            itemView.animate()
                .translationX(targetTranslationX)
                .setDuration(SWIPE_ANIMATION_DURATION)
                .withEndAction { recyclerView.invalidateItemDecorations() }
                .start()
        }

        private fun resolveReportInfo(holder: RecyclerView.ViewHolder): ReportInfo? {
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return null
            return adapter.peek(position)
        }

        private fun isTouchingDeleteButton(
            event: MotionEvent,
            holder: RecyclerView.ViewHolder
        ): Boolean {
            val itemView = holder.itemView
            val deleteWidth = getDeleteWidth(itemView.height)
            return deleteWidth > 0 &&
                event.x >= itemView.right - deleteWidth &&
                event.x <= itemView.right &&
                event.y >= itemView.top &&
                event.y <= itemView.bottom
        }

        private fun drawDeleteView(canvas: Canvas, itemView: View) {
            val width = getDeleteWidth(itemView.height)
            if (width <= 0) return
            val bitmap = deleteBitmap ?: return
            canvas.drawBitmap(
                bitmap,
                (itemView.right - width).toFloat(),
                itemView.top.toFloat(),
                null
            )
        }

        private fun getDeleteWidth(height: Int): Int {
            if (height <= 0) return deleteWidth
            if (deleteWidth > 0 && deleteBitmapHeight == height) return deleteWidth
            val targetWidth = deleteView.layoutParams?.width?.takeIf { it > 0 } ?: 0
            if (targetWidth <= 0) return 0
            val widthSpec = View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            deleteView.measure(widthSpec, heightSpec)
            deleteWidth = targetWidth
            deleteView.layout(0, 0, deleteWidth, height)
            deleteBitmap?.recycle()
            deleteBitmap = Bitmap.createBitmap(deleteWidth, height, Bitmap.Config.ARGB_8888)
            deleteView.draw(Canvas(deleteBitmap!!))
            deleteBitmapHeight = height
            return deleteWidth
        }

        private companion object {
            const val SWIPE_ANIMATION_DURATION = 180L
        }
    }
}
