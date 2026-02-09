package com.kgmedia.fastfilm.feature.play.manger

import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.kgmedia.fastfilm.core.network.apis.entity.WorkInfo
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

    // 页面切换回调：(当前数据, 当前位置) -> Unit
    var onPageChanged: ((WorkInfo, Int) -> Unit)? = null

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

    // 获取当前选中的 Item
    fun getCurrentItem(): WorkInfo? {
        val pos = getCurrentPosition() // 现有的方法
        if (pos in 0 until adapter.itemCount) {
            return adapter.peek(pos)
        }
        return null
    }

    private fun setupFirstLoadListener() {
        // [修改方案] 拆分触发逻辑：
        // 1. 定义一个通用的检查并播放的方法
        val checkAndPlay = {
            if (isFirstLoad && adapter.itemCount > 0) {
                Logger.d("FeedListController", "AutoPlay triggered! itemCount=${adapter.itemCount}")
                // 立即锁定，防止重复触发
                isFirstLoad = false

                recyclerView.post {
                    if (recyclerView.isAttachedToWindow) {
                        Logger.d(
                            "FeedListController",
                            "targetInitialPosition=$targetInitialPosition adapter.itemcount = ${adapter.itemCount}"
                        )
                        if (targetInitialPosition != -1 && targetInitialPosition < adapter.itemCount) {
                            // 1. 直接闪现到指定位置
                            layoutManager.scrollToPositionWithOffset(targetInitialPosition, 0)
                            // 2. 等待布局稳定后播放
                            recyclerView.post { playCurrentItem() }
                        } else {
                            // 默认播放第 0 个
                            playCurrentItem()
                        }
                    }
                }
            }
        }

        // 2. 监听 LoadState (保留，用于兼容网络加载的Loading->NotLoading状态切换)
        adapter.addLoadStateListener { loadState ->
            Logger.d("FeedListController", "LoadState: ${loadState.refresh}, Count: ${adapter.itemCount}")
            if (loadState.refresh is LoadState.NotLoading) {
                checkAndPlay.invoke()
            }
        }

        // 3. [核心修复] 监听数据页面更新 (专门解决 PagingData.from 内存数据不触发 LoadState 变化的问题)
        adapter.addOnPagesUpdatedListener {
            Logger.d("FeedListController", "PagesUpdated! Count: ${adapter.itemCount}")
            checkAndPlay.invoke()
        }
    }

    fun playCurrentItem() {
        val centerView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(centerView)

        if (position < 0 || position >= adapter.itemCount) return

        val item = adapter.peek(position) ?: return

        onPageChanged?.invoke(item, position)
        val holder =
            recyclerView.getChildViewHolder(centerView) as? VideoFeedAdapter.VideoViewHolder
                ?: return

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