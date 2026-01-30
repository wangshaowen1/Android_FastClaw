package com.kgmedia.fastfilm.feature.play.manger

import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.feature.play.VideoFeedAdapter
import com.nhsd.basic.utils.Logger

/**
 * 列表控制器：负责滑动监听、自动播放、预加载
 */
class FeedListController(
    private val recyclerView: RecyclerView,
    private val adapter: VideoFeedAdapter,
    private val playerManager: FeedPlayerManager
) {
    private val layoutManager = LinearLayoutManager(recyclerView.context)
    private val snapHelper = PagerSnapHelper()

    // 标记是否是第一次加载数据
    private var isFirstLoad = true

    // 目标初始位置，默认为 -1 (不跳转)
    private var targetInitialPosition: Int = -1

    init {
        recyclerView.layoutManager = layoutManager
        snapHelper.attachToRecyclerView(recyclerView)

        // 1. 滑动监听
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    playCurrentItem()
                }
            }
        })

        // 2. 初始化首屏监听 (只注册一次！)
        setupFirstLoadListener()
    }

    /**
     * 设置初始播放位置
     * 注意：这个方法必须在 adapter 数据提交之前（onCreate 中）调用
     */
    fun setInitialPosition(position: Int) {
        this.targetInitialPosition = position
    }

    private fun setupFirstLoadListener() {
        adapter.addLoadStateListener { loadState ->
            // 当数据刷新完成 (NotLoading) 且是第一次加载时触发
            if (loadState.refresh is LoadState.NotLoading && isFirstLoad) {
                if (adapter.itemCount > 0) {
                    // 立即锁定，防止后续 LoadState 变化重复触发
                    isFirstLoad = false

                    recyclerView.post {
                        if (recyclerView.isAttachedToWindow) {
                            // [核心修复] 在这里统一处理跳转逻辑
                            Logger.d("FeedListController", "targetInitialPosition=$targetInitialPosition adapter.itemcount = ${adapter.itemCount}")
                            if (targetInitialPosition != -1 && targetInitialPosition < adapter.itemCount) {
                                // 1. 直接闪现到指定位置 (使用 scrollToPositionWithOffset 确保准确置顶)
                                layoutManager.scrollToPositionWithOffset(targetInitialPosition, 0)

                                // 2. 再次 Post，等待 RecyclerView 布局稳定后，寻找 SnapView 并播放
                                recyclerView.post {
                                    playCurrentItem()
                                }
                            } else {
                                // 默认逻辑：播放第 0 个
                                playCurrentItem()
                            }
                        }
                    }
                }
            }
        }
    }

    fun playCurrentItem() {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(centerView)

        if (position < 0 || position >= adapter.itemCount) return

        val item = adapter.peek(position) ?: return
        val holder = recyclerView.getChildViewHolder(centerView) as? VideoFeedAdapter.VideoViewHolder ?: return

        // 调用 Manager 播放
        playerManager.attachAndPlay(item, holder.binding.playerView)
        // 强制更新 UI 为播放状态
        holder.updatePlayState(true)

        // 预加载下一条
        if (position + 1 < adapter.itemCount) {
            val nextItem = adapter.peek(position + 1)
            // 根据你的 TrackInfo/DramaInfo 结构调用正确的 url 方法
            // 假设是 getPlayUrl() 或 playUrl
            nextItem?.getPlayUrl()?.let { playerManager.preload(it) }
        }
    }

    fun smoothScrollToNext(): Boolean {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return false
        val currentPos = layoutManager.getPosition(centerView)

        if (currentPos < adapter.itemCount - 1) {
            recyclerView.smoothScrollToPosition(currentPos + 1)
            return true
        }
        return false
    }

    fun getCurrentPosition(): Int {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return -1
        return layoutManager.getPosition(centerView)
    }
}