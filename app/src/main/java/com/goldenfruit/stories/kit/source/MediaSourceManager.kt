package com.kgmedia.fastfilm.kit.source

import com.kgmedia.fastfilm.common.PagedProxy
import com.kgmedia.fastfilm.core.network.apis.ApiException
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.kit.PlaylistListener
import com.kgmedia.fastfilm.kit.PlaylistPlayer
import com.kgmedia.fastfilm.kit.ioScope
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton object to manage media source loading and playlist updates.
 */
class MediaSourceManager : PlaylistListener {

    companion object {
        private const val LOAD_THRESHOLD_SIZE = 2
        const val LOG_TAG = "MediaSourceManager"
        private val instances = ConcurrentHashMap<String, MediaSourceManager>() // 使用线程安全的Map

        fun with(
            player: PlaylistPlayer,
            initialMediasIsCache: Boolean = false
        ): MediaSourceManager {
            val playerId = getPlayerUniqueId(player)
            return instances.getOrPut(playerId) {
                MediaSourceManager().apply {
                    setupPlayer(player, initialMediasIsCache)
                }
            }
        }

        fun get(player: PlaylistPlayer): MediaSourceManager? {
            return instances[getPlayerUniqueId(
                player
            )]
        }

        fun release(player: PlaylistPlayer) {
            val playerId = getPlayerUniqueId(player)
            instances[playerId]?.release()
            instances.remove(playerId)
        }

        private fun getPlayerUniqueId(player: PlaylistPlayer): String {
            return "${player.javaClass.simpleName}_${System.identityHashCode(player)}"
        }
    }

    private var job: Job? = null

    // 新增：记录最近一次加载失败的页面
    private var lastFailedPage: Int = -1

    // 数据流
    private val _allLoadedMedias = MutableStateFlow<List<ShortDramaInfo>>(emptyList())
    val allLoadedMedias: StateFlow<List<ShortDramaInfo>> = _allLoadedMedias.asStateFlow()

    // replay 必须为 0，防止 PagingSource 读到上一页的旧数据
    // extraBufferCapacity = 1 允许发射者在没有订阅者时缓存 1 条数据（避免挂起）
    private val _newPageDataFlow = MutableSharedFlow<List<ShortDramaInfo>>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val newPageDataFlow: SharedFlow<List<ShortDramaInfo>> = _newPageDataFlow.asSharedFlow()

    // 错误流
    private val _loadErrorFlow = MutableSharedFlow<Throwable>(replay = 0)
    val loadErrorFlow: SharedFlow<Throwable> = _loadErrorFlow.asSharedFlow()

    private val _preparedMediaDetails = MutableStateFlow<Map<String, ShortDramaInfo>>(emptyMap())
    val preparedMediaDetails: StateFlow<Map<String, ShortDramaInfo>> =
        _preparedMediaDetails.asStateFlow()

    // 新增：正在进行的加载任务
    private val loadingJobs = mutableMapOf<String, Job>()
    private var selectionDialogData: List<ShortDramaInfo> = emptyList()

    var mediaSource: ShortsPageSource? = null

    var playableSource: ShortDetailSource? = null
    var shortListPlayer: PlaylistPlayer? = null
    private val _currentPreparingMedia = MutableSharedFlow<ShortDramaInfo>()
    val currentPreparingMedia: SharedFlow<ShortDramaInfo> = _currentPreparingMedia.asSharedFlow()
    private val proxy = PagedProxy()

    fun hasNextPage(): Boolean = proxy.hasNextPage()

    fun setupPlayer(
        player: PlaylistPlayer,
        initialMediasIsCache: Boolean = false
    ): MediaSourceManager {
        if (this.shortListPlayer == player) {
            return this
        }
        this.shortListPlayer?.removeListener(this)
        player.addListener(this)
        this.shortListPlayer = player
        setInitialMedias(player.playlist, initialMediasIsCache)
        return this
    }

    fun setInitialMedias(
        initialMedias: List<ShortDramaInfo>?,
        initialMediasIsCache: Boolean = false
    ): MediaSourceManager {
        initialMedias?.let {
            if (shortListPlayer?.playlist != initialMedias) {
                shortListPlayer?.playlist = initialMedias
            }
            updateAllLoadedMedias(initialMedias, false)
            selectionDialogData = initialMedias
        } ?: run {
            updateAllLoadedMedias(emptyList(), false)
        }
        lastFailedPage = -1
        proxy.reset()
        if (initialMediasIsCache) {
            proxy.currentPage = 0
        } else {
            val totalSize = initialMedias?.size ?: 0
            val pageSize = proxy.pageSize
            val currentPage = totalSize / pageSize
            proxy.currentPage = currentPage
        }
        return this
    }
    /**
     * @param pageSize 每页加载的数量
     */
    fun setPageSize(pageSize: Int): MediaSourceManager {
        proxy.pageSize = pageSize
        return this
    }
    // 重试方法
    fun retry() {
        if (lastFailedPage == -1 || job?.isActive == true) return
        Logger.i(LOG_TAG, "重试加载失败的页面: $lastFailedPage")
        loadNextPage()
    }

    fun refresh() {
        resetState()
        loadNextPage()
    }
    fun resetState(sameSource: Boolean = false) {
        job?.cancel() // 取消之前的任务
        loadingJobs.values.forEach { it.cancel() } // 取消详情加载任务
        loadingJobs.clear()

        proxy.currentPage = 0
        lastFailedPage = -1
        _allLoadedMedias.value = emptyList() // 清空数据流
        selectionDialogData = emptyList()

        if (!sameSource) {
            _preparedMediaDetails.value = emptyMap()
            // 3. ✅ 【建议】重置播放器的列表，防止旧数据残留
            shortListPlayer?.setPlaylist(emptyList())
        }
        Logger.i(LOG_TAG, "重置，当前页: ${proxy.currentPage}, 每页大小: ${proxy.pageSize}")
    }
    fun setMediaSource(mediaSource: ShortsPageSource?): MediaSourceManager {
        job?.cancel()
        // ✅ 核心修复：检查数据源是否发生变更
        val isDifferentSource = this.mediaSource?.sourceId != mediaSource?.sourceId

        this.mediaSource = mediaSource

        mediaSource?.let {
            // 如果数据源变了（例如从剧集B切回剧集A），或者当前没有数据
            // 必须重置状态，防止 B 的数据残留在 A 的 Manager 中
            if (isDifferentSource || _allLoadedMedias.value.isEmpty()) {
                Logger.i(LOG_TAG, "数据源变更或初始化，重置状态: ${mediaSource.sourceId}")
                // 这里调用 resetState 清理 _allLoadedMedias 和 proxy
                resetState()
                loadNextPage()
            } else {
                // 如果是同一个数据源（例如只是息屏再亮屏），则检查是否需要加载更多
                checkAndLoadIfNeeded()
            }
        }
        return this
    }

    fun setPlayableSource(shortSource: ShortDetailSource?): MediaSourceManager {
        this.playableSource = shortSource
        return this
    }

    private fun checkAndLoadIfNeeded() {
        shortListPlayer?.let { player ->
            val playlistSize = player.playlist.size
            val currentPlayingIndex = player.getCurrentPlayingIndex()
            if (playlistSize - currentPlayingIndex <= LOAD_THRESHOLD_SIZE) {
                if (proxy.hasNextPage() && job?.isActive != true) {
                    Logger.i(LOG_TAG, "接近列表末尾，自动加载更多")
                    loadNextPage()
                }
            }
        }
    }

    suspend fun loadNextPageAndAwait(): List<ShortDramaInfo> {
        // 1. 如果没有下一页了，直接返回空
        if (!proxy.hasNextPage()) return emptyList()

        // 2. 发起加载
        if (job?.isActive != true) {
            loadNextPage()
        }

        // 3. 等待结果
        return try {
            withTimeout(10000) { // 加上超时保护
                _newPageDataFlow.first()
            }
        } catch (e: Exception) {
            // 如果超时或出错，返回空列表，Paging3 会根据 LoadResult 决定是否重试或停止
            emptyList()
        }
    }

    fun mergeSelectionData(newData: List<ShortDramaInfo>) {
        selectionDialogData = newData
        val currentAllData = _allLoadedMedias.value.toMutableList()
        val newItems = newData.filterNot { newItem ->
            currentAllData.any { it.dramasId == newItem.dramasId }
        }

        if (newItems.isNotEmpty()) {
            currentAllData.addAll(newItems)
            updateAllLoadedMedias(currentAllData, false)
            shortListPlayer?.let { player ->
                if (player.playlist.size < currentAllData.size) {
                    player.addToPlaylist(newItems)
                }
            }
        }
    }

    fun getSelectionDialogInitialData(): List<ShortDramaInfo> {
        return selectionDialogData.ifEmpty { _allLoadedMedias.value }
    }

    override fun onShortChanged(short: ShortDramaInfo?) {
        super.onShortChanged(short)
        checkAndLoadIfNeeded()

        // 预加载下一个视频的详情
        val nextIndex = (shortListPlayer?.getCurrentPlayingIndex() ?: 0) + 1
        val nextShort = _allLoadedMedias.value.getOrNull(nextIndex)
        ensurePlayableMedia(nextShort?.dramasId)
    }


    /**
     * 确保媒体可播放，支持并发请求，并维护数据一致性
     */
    fun ensurePlayableMedia(shortDramaId: String?, forceRefresh: Boolean = false) {
        if (shortDramaId == null) return

        val dramaId = shortDramaId

        // 如果已经加载过，直接返回
        if (_preparedMediaDetails.value.containsKey(dramaId) && !forceRefresh) {
            return
        }

        // 如果已经在加载中，不重复启动
        if (loadingJobs.containsKey(dramaId) && !forceRefresh) {
            return
        }

        // 启动新的加载任务
        val job = ioScope.launch {
            try {
                val response = playableSource?.loadDetail(dramaId)
                if (response?.isSuccess == true) {
                    response.data?.let { detail ->
                        // 更新所有相关数据源，保持一致性
                        updateAllDataSourcesWithDetail(detail)
                        Logger.i(LOG_TAG, "详情加载成功: ${detail.dramasId}")
                    }
                } else {
                    // 加载失败，但仍然清理任务
                    Logger.w(LOG_TAG, "详情加载失败: $dramaId, 响应: $response")
                }
            } catch (error: Throwable) {
                Logger.e(LOG_TAG, "详情加载异常: $dramaId, error: $error")
            } finally {
                // 无论成功失败，都清理加载任务
                loadingJobs.remove(dramaId)
            }
        }

        loadingJobs[dramaId] = job
    }


    fun updateAllDataSourcesWithDetail(detailedShort: ShortDramaInfo) {
        val dramaId = detailedShort.dramasId

        // 1. 更新 preparedMediaDetails
        val currentPreparedMap = _preparedMediaDetails.value.toMutableMap()
        currentPreparedMap[dramaId] = detailedShort
        _preparedMediaDetails.value = currentPreparedMap

        // 2. 更新 allLoadedMedias
        updateAllLoadedMediasWithDetail(detailedShort)

        // 3. 更新播放列表（如果存在）
        updatePlaylistWithDetail(detailedShort)

        Logger.i(LOG_TAG, "数据一致性更新完成: $dramaId")
    }

    /**
     * 在 allLoadedMedias 中更新详情
     */
    private fun updateAllLoadedMediasWithDetail(detailedShort: ShortDramaInfo) {
        val currentList = _allLoadedMedias.value
        val updatedList = currentList.map { item ->
            if (item.dramasId == detailedShort.dramasId) detailedShort else item
        }

        // 只有当列表确实发生变化时才更新
        if (updatedList != currentList) {
            _allLoadedMedias.value = updatedList
            Logger.i(LOG_TAG, "allLoadedMedias 更新: ${detailedShort.dramasId}")
        }
    }

    /**
     * 在播放列表中更新详情
     */
    private fun updatePlaylistWithDetail(detailedShort: ShortDramaInfo) {
        shortListPlayer?.let { player ->
            val currentPlaylist = player.playlist ?: return
            val index = currentPlaylist.indexOfFirst { it.dramasId == detailedShort.dramasId }
            if (index != -1) {
                try {
                    if (currentPlaylist is MutableList) {
                        currentPlaylist[index] = detailedShort
                    } else {
                        val updatedPlaylist = currentPlaylist.toMutableList()
                        updatedPlaylist[index] = detailedShort
                        player.setPlaylist(updatedPlaylist)
                    }
                    Logger.i(LOG_TAG, "播放列表更新成功: ${detailedShort.dramasId} at index $index")
                } catch (e: Exception) {
                    Logger.e(LOG_TAG, "播放列表更新失败: ${e.message}")
                }
            } else {
                Logger.w(LOG_TAG, "播放列表中未找到对应ID，跳过更新: ${detailedShort.dramasId}")
            }
        }
    }

    /**
     * 取消指定短剧的加载
     */
    fun cancelLoading(dramaId: String) {
        loadingJobs[dramaId]?.cancel()
        loadingJobs.remove(dramaId)
    }

    /**
     * 批量确保媒体可播放
     */
    fun ensurePlayableMedias(shorts: List<ShortDramaInfo>) {
        shorts.forEach { ensurePlayableMedia(it.dramasId) }
    }

    private fun loadNextPage(): Boolean {
        if (!proxy.hasNextPage()) return false
        job?.cancel()
        proxy.toNextPage()
        job = ioScope.launch {
            try {
                val currentPage = proxy.currentPage
                Logger.i(LOG_TAG, "加载页面: $currentPage, 每页大小: ${proxy.pageSize} ,media source=${mediaSource}")

                val response = mediaSource?.nextPage(currentPage, proxy.pageSize)
                if (response?.isSuccess != true || response.data == null) {
                    throw ApiException(response?.code ?: 0, response?.msg ?: "Unknown error")
                }
                val data = response.data
                val items = data?.items ?: emptyList()

                Logger.i(LOG_TAG, "加载完成: $currentPage, 数量: ${items.size}")
                lastFailedPage = -1
                if (!proxy.isPageCountSet) {
                    data?.totalSize?.let { proxy.dataCount = it }
                }

                if (items.isEmpty()) {
                    proxy.setReachEnd(true)
                }
                processLoadedItems(currentPage, items)
            } catch (error: Throwable) {
                error.printStackTrace()
                lastFailedPage = proxy.currentPage
                Logger.i(LOG_TAG, "加载错误: ${error.cause}")
                proxy.toPrePage()
                // 发射错误到 Flow
                _loadErrorFlow.emit(error)
            }
        }

        return true
    }

    private suspend fun processLoadedItems(currentPage: Int, items: List<ShortDramaInfo?>) {
        val validItems = items.filterNotNull()
        val existingIds = _allLoadedMedias.value.map { it.dramasId }.toSet()
        val newItems = validItems.filterNot { item ->
            item.dramasId in existingIds
        }.distinctBy { it.dramasId }
        Logger.i(LOG_TAG, "加载条数=${validItems.size}, 去重后=${newItems.size}")
        if (newItems.isEmpty() && proxy.hasNextPage()) {
            Logger.i(LOG_TAG, "页面无新数据，尝试下一页")
            loadNextPage()
        } else {
            updateAllLoadedMedias(
                _allLoadedMedias.value + newItems, proxy.currentPage == 1
            )
            shortListPlayer?.addToPlaylist(newItems)
            Logger.i(LOG_TAG, "更新数据，发送更新事件")
            _newPageDataFlow.emit(newItems)
        }
    }

    private fun updateAllLoadedMedias(newData: List<ShortDramaInfo>, prepareFirst: Boolean) {
        _allLoadedMedias.value = newData
        if (prepareFirst) {
            ensurePlayableMedia(newData.firstOrNull()?.dramasId)
        }
    }

    fun getCurrentPage(): Int = proxy.currentPage

    fun getPageSize(): Int = proxy.pageSize

    fun removeMediaById(dramaId: String?) {
        dramaId ?: return

        Logger.i(LOG_TAG, "开始移除短剧: $dramaId")

        // 1. 取消正在进行的加载任务
        cancelLoading(dramaId)

        // 2. 从 preparedMediaDetails 中移除
        val currentPreparedMap = _preparedMediaDetails.value.toMutableMap()
        currentPreparedMap.remove(dramaId)
        _preparedMediaDetails.value = currentPreparedMap

        // 3. 从 allLoadedMedias 中移除
        val currentAllLoaded = _allLoadedMedias.value.toMutableList()
        currentAllLoaded.removeAll { it.dramasId == dramaId }
        _allLoadedMedias.value = currentAllLoaded

        // 4. 从播放列表中移除
        shortListPlayer?.let { player ->
            val currentPlaylist = player.playlist?.toMutableList()
            currentPlaylist?.removeAll { it.dramasId == dramaId }

            // 检查是否移除了当前正在播放的短剧
            val wasCurrentPlaying = player.currentShort()?.dramasId == dramaId

            player.setPlaylist(currentPlaylist)

            // 如果移除了当前播放的短剧，停止播放
            if (wasCurrentPlaying) {
                player.stop()
            }
        }

        // 5. 从选择对话框数据中移除
        val currentSelectionData = selectionDialogData.toMutableList()
        currentSelectionData.removeAll { it.dramasId == dramaId }
        selectionDialogData = currentSelectionData

        // 6. 如果移除了分页数据中的项目，可能需要调整分页状态
        // 注意：这里不重置分页，因为可能还有其他数据

        Logger.i(LOG_TAG, "短剧移除完成: $dramaId")
    }

    /**
     * 批量移除多个短剧
     */
    fun removeMediasByIds(dramaIds: List<String>) {
        dramaIds.forEach { removeMediaById(it) }
    }

    /**
     * 清空所有与指定系列相关的短剧（可选功能）
     */
    fun removeMediasBySeries(seriesId: String?) {
        if (seriesId == null) return

        val mediasToRemove = _allLoadedMedias.value
            .filter { it.dramasSeries?.dramasSeriesId == seriesId }
            .map { it.dramasId }

        removeMediasByIds(mediasToRemove)
    }

    fun release() {
        val playerId = shortListPlayer?.let { getPlayerUniqueId(it) }

        Logger.i(LOG_TAG, "Releasing MediaSourceManager for player: $playerId")

        // 取消所有任务
        job?.cancel()
        loadingJobs.values.forEach { it.cancel() }
        loadingJobs.clear()

        // 移除监听器
        shortListPlayer?.removeListener(this)

        // 重置状态
        proxy.reset()
        mediaSource = null
        playableSource = null

        // 清空数据
        _allLoadedMedias.value = emptyList()
        _preparedMediaDetails.value = emptyMap()
        selectionDialogData = emptyList()

        // 从实例映射中移除
        playerId?.let { instances.remove(it) }
        shortListPlayer = null

        Logger.i(LOG_TAG, "MediaSourceManager released completely")
    }

}