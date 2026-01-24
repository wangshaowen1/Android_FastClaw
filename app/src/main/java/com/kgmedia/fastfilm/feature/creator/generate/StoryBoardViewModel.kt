package com.kgmedia.fastfilm.feature.creator.generate

import android.os.SystemClock
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.R
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.adapter.QuickAdapter
import com.kgmedia.fastfilm.core.Events.ScriptContentGenerateSuccessEvent
import com.kgmedia.fastfilm.core.Global
import com.kgmedia.fastfilm.core.db.entity.SSEMessage
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.SSEvent
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateBody
import com.kgmedia.fastfilm.core.network.apis.request.InputContentBody
import com.kgmedia.fastfilm.core.network.apis.request.PopularRecommendDramasIdBody
import com.kgmedia.fastfilm.core.network.apis.response.StoryboardListData
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * 故事板视图模型，负责处理故事板生成相关的业务逻辑
 * 包括SSE内容生成、角色列表获取、分镜列表获取等功能
 */
class StoryBoardViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "StoryBoardViewModel"
        const val DISPLAY_LOADING = 0
        const val DISPLAY_FAIL = 1
        const val DISPLAY_CONTENT = 2

        const val DISPLAY_STORYBOARD_LOADING = 0
        const val DISPLAY_STORYBOARD_FAIL = -1
        const val DISPLAY_STORYBOARD_CONTENT = 1
    }

    /**
     * 续写时是剧集id，同款是剧本id
     */
    var otherExtraId: String? = null
    var content: String? = null
    var model: Int? = null
    var videoRatio: Int? = null
    var dramaStyleId: String? = null

    var mSSEContent = ObservableField<String>()
    var dramaId = MutableLiveData<String>()
    var mContent = ObservableField<String>()
    var mDramaName = MutableLiveData<String>()
    var mRoleList = MutableLiveData<List<CharacterInfo>>()

    val displayChild = ObservableInt(DISPLAY_LOADING)
    val loadingState = MutableLiveData(0)
    val loadingStoryboardState = MutableLiveData(DISPLAY_STORYBOARD_LOADING)

    var mStoryboardListData = MutableLiveData<StoryboardListData?>()
    var isFirstGenerate = ObservableBoolean(false)

    val adapter by lazy { QuickAdapter<ItemStoryboardVM>(R.layout.item_storyboard_info, emptyList()) }

    private var loopRoleListJob: Job? = null
    private var initLoadingJon: Job? = null
    private var loopGeneratingSegmentsJob: Job? = null

    val totalStoryBoardCount by lazy { ObservableInt(0) }
    val loadingProgress by lazy { ObservableInt(0) }
    val inputAllComplete by lazy { ObservableBoolean(false) }
    val nextStepEnabled by lazy { ObservableBoolean(false) }

    private var fakeProgressJob: Job? = null

    var showStoryboardEditor: ((index: Int, vM: ItemStoryboardVM) -> Unit)? = null

    private var sseChatJob: Job? = null

    /**
     * 启动SSE内容生成过程
     * 根据model类型选择不同的API接口进行内容生成
     */
    fun sseGenerate() {
        sseChatJob = launch {
            try {
                val sseFlow = if (model == DramaGenerateBody.GEN_WAY_MAKE_SAME_STORY) {
                    mRepo.api.sseMakeSameContentGenerate(PopularRecommendDramasIdBody(otherExtraId))
                } else {
                    mRepo.api.sseContentGenerate(InputContentBody(model, content))
                }
                Logger.d("[SSE]", "content body = $content")
                Logger.d("[SSE]", "otherExtraId body = $otherExtraId")
                sseFlow.collect { event ->
                    handleSSEvent(event)
                }
            } catch (e: CancellationException) {
                Logger.d("[SSE]", "主动取消协程")
                throw e // 重新抛出以确保取消流程完整
            } catch (e: Exception) {
                Log.e("[SSE]", "sse异常断开: e = ${e.fillInStackTrace()}")
                displayChild.set(DISPLAY_FAIL)
                e.fillInStackTrace()
            } finally {
                releaseJob()
            }
        }
    }

    /**
     * 处理SSE事件
     * @param event SSE事件对象，包括Open、Message、Error、Close等类型
     */
    private fun handleSSEvent(event: SSEvent) {
        when (event) {
            is SSEvent.Open -> {
                displayChild.set(DISPLAY_LOADING)
                Logger.d("[SSE]", "Open")
                sseBuffer = StringBuilder()
                mSSEContent.set("")
                mContent.set("")
            }

            is SSEvent.Message -> {
                val data = event.data
                try {
                    val sseMessage = Json.decodeFromString<SSEMessage>(data)
                    if (sseMessage.code == SSEMessage.TYPE_ERROR) {
                        sseMessage.content?.showToast()
                        sseError()
                    } else {
                        Logger.d("[SSE]", "receive msg: parsing content message $sseMessage")
                        handlerSSEMessage(sseMessage)
                    }
                } catch (e: Exception) {
                    Log.e("[SSE]", "receive msg: parsing error for $data", e)
                    sseError()
                }
            }

            is SSEvent.Error -> {
                sseError()
                Log.e("[SSE]", "sse协议出错: ${event.throwable.fillInStackTrace()}")
            }

            is SSEvent.Close -> {
                Log.e("[SSE]", "sse协议 关闭")
                releaseJob()
            }
        }
    }

    private var sseBuffer: StringBuilder? = null
    private var lastUiEmitTime = 0L

    /**
     * 处理SSE消息
     * @param sseMessage SSE消息对象
     */
    fun handlerSSEMessage(sseMessage: SSEMessage) {
        if (sseMessage.isOver()) {
            emitToUi(sseBuffer.toString(), true)
            sseBuffer?.clear()
            dramaGenerate()
            return
        }
        sseMessage.content?.let { content ->
            sseBuffer?.append(content)
            emitToUi(sseBuffer?.toString())
        }
    }

    private fun emitToUi(content: String?, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastUiEmitTime < 50L) return
        lastUiEmitTime = now
        mSSEContent.set(content)
    }

    /**
     * SSE错误处理
     */
    private fun sseError() {
        displayChild.set(DISPLAY_FAIL)
        sseChatJob?.cancel()
        sseChatJob = null
    }

    /**
     * 释放SSE相关资源
     */
    private fun releaseJob() {
        sseChatJob?.cancel()
        sseChatJob = null
    }

    /**
     * 执行剧本生成
     */
    fun dramaGenerate() {
        mContent.set(mSSEContent.get())
        displayChild.set(DISPLAY_CONTENT)
        launch {
            response(mRepo.dramaGenerate(DramaGenerateBody(mSSEContent.get(), genWay = model, styleId = dramaStyleId, videoRatio = videoRatio)), {
                if (model == DramaGenerateBody.GEN_WAY_CONTINUE_STORY) {
                    Global.clearContinueInputContent(otherExtraId)
                    FlowEventBus.postSync(
                        ScriptContentGenerateSuccessEvent(
                            ScriptContentGenerateSuccessEvent.TYPE_CONTINUE_WRITE,
                            otherExtraId
                        )
                    )
                } else if (model != DramaGenerateBody.GEN_WAY_MAKE_SAME_STORY) {
                    Global.clearInputContent()
                    FlowEventBus.postSync(
                        ScriptContentGenerateSuccessEvent(
                            if (model == DramaGenerateBody.GEN_WAY_INSPIRATION) ScriptContentGenerateSuccessEvent.TYPE_INSPIRATION else ScriptContentGenerateSuccessEvent.TYPE_CUSTOMIZED,
                            null
                        )
                    )
                }
                dramaId.value = data?.dramasId ?: ""
                getRoleList()
                queryStoryboardList()
                report()
            }) {
                this?.errorMsg?.showToast()
                displayChild.set(DISPLAY_FAIL)
            }
        }
    }

    /**
     * 从app主界面点击继续完成过来走这里进行恢复
     */
    fun restoreDramaGenerate() {
        displayChild.set(DISPLAY_CONTENT)
        getRoleList()
        queryStoryboardList(true)
    }

    /**
     * 上报来源，仅续写和同款上报
     */
    fun report() {
        val isMakeSame = model == DramaGenerateBody.GEN_WAY_CONTINUE_STORY
        if (isMakeSame || model == DramaGenerateBody.GEN_WAY_MAKE_SAME_STORY) {
            launch {
                response(mRepo.dramaGenerateSourceReport(dramaId.value, if (isMakeSame) 1 else 2, otherExtraId)) {}
            }
        }
    }

    /**
     * 获取角色列表
     */
    fun getRoleList() {
        launch {
            response(mRepo.dramaRolesPolling(dramaId.value)) {
                if (data?.dramasName?.isNotEmpty() == true) {
                    mDramaName.value = data?.dramasName ?: ""
                }
                if (data?.roleList?.isNotEmpty() == true) {
                    mRoleList.value = data?.roleList!!
                }
                if (data?.roleList.isNullOrEmpty() || data?.dramasName.isNullOrEmpty()) {
                    loopGetRoleList()
                    return@response
                }
            }
        }
    }

    /**
     * 循环获取角色列表
     */
    fun loopGetRoleList() {
        loopRoleListJob?.cancel()
        loopRoleListJob = launch {
            delay(3000)
            getRoleList()
        }
    }

    /**
     * 分镜列表加载的loading
     */
    fun initLoadingState() {
        initLoadingJon?.cancel()
        initLoadingJon = viewModelScope.launch {
            loadingState.value = 0
            delay(3000)
            loadingState.value = 1
            delay(3000)
            loadingState.value = 2
            delay(3000)
            loadingState.value = 3
        }
    }

    fun skipLoadingStart() {
        initLoadingJon?.cancel()
        loadingState.value = 3
        loadingProgress.set(99)
    }

    fun skipLoadingEnd() {
        initLoadingJon?.cancel()
        loadingState.value = 3
    }

    /**
     * 启动假进度
     * 每秒增加2%进度，直到99%后保持不变直到内容返回
     */
    private fun startFakeProgress() {
        fakeProgressJob?.cancel()
        fakeProgressJob = viewModelScope.launch {
            loadingProgress.set(0)
            while (loadingProgress.get() < 99) {
                delay(1000) // 1秒
                loadingProgress.set(loadingProgress.get() + 2) // 每秒增加2%
                if (loadingProgress.get() >= 99) {
                    loadingProgress.set(99)
                    break
                }
            }
            // 保持99%直到内容返回
            while (loadingProgress.get() == 99) {
                delay(1000) // 保持检查但不增加进度
            }
        }
    }

    /**
     * 停止假进度并快速完成到100%
     */
    private fun finishFakeProgress() {
        fakeProgressJob?.cancel()
        skipLoadingEnd()
        viewModelScope.launch {
            if (loadingProgress.get() < 100) {
                // 快速增加到100%
                while (loadingProgress.get() < 100) {
                    loadingProgress.set(loadingProgress.get() + 1)
                    delay(20) // 快速增加
                }
                loadingStoryboardState.value = DISPLAY_STORYBOARD_CONTENT
            } else {
                loadingStoryboardState.value = DISPLAY_STORYBOARD_CONTENT
            }
        }
    }

    /**
     * 停止假进度并直接完成
     */
    private fun finishProgress() {
        fakeProgressJob?.cancel()
        skipLoadingEnd()
        loadingStoryboardState.value = DISPLAY_STORYBOARD_CONTENT
    }

    /**
     * 更新故事内容
     * @param content 新的故事内容
     */
    fun refreshDramaContent(content: String?) {
        mContent.set(content)
        loadingStoryboardState.value = DISPLAY_STORYBOARD_LOADING
        nextStepEnabled.set(false)
        adapter.submitList(emptyList())
        queryStoryboardList()
    }

    /**
     * 查询分镜列表
     */
    fun queryStoryboardList(isRestore: Boolean = false) {
        if (isRestore) {
            skipLoadingStart()
        } else {
            initLoadingState()
            startFakeProgress()
        }
        getStoryBoardList(isRestore)
    }

    /**
     * 获取分镜列表
     */
    fun getStoryBoardList(isRestore: Boolean = false) {
        launch({
            val result = mRepo.getStoryBoardList(dramaId.value)
            if (result.isSuccess) {
                val data = result.data
                if (data?.isGenSuccess() == true) {
                    applyStoryboard(data, isRestore)
                } else if (data?.isGenFailed() == true) {
                    loadingStoryboardState.value = DISPLAY_STORYBOARD_FAIL
                    skipLoadingEnd()
                } else {
                    loopStoryBoardList()
                }
            } else {
                loopStoryBoardList()
            }
        }, onError = {
            loopStoryBoardList()
        })
    }

    /**
     * 分镜列表加载失败重试
     */
    fun retryStoryboardGeneration() {
        launchWithLoading(true) {
            response(mRepo.getStoryBoardListRetry(dramaId.value)) {
                adapter.submitList(emptyList())
                queryStoryboardList()
            }
        }
    }

    /**
     * 循环获取分镜列表
     */
    fun loopStoryBoardList() {
        loopGeneratingSegmentsJob?.cancel()
        loopGeneratingSegmentsJob = launch {
            delay(3000)
            getStoryBoardList()
        }
    }

    /**
     * 应用分镜数据
     * @param info 分镜列表数据
     */
    private fun applyStoryboard(info: StoryboardListData, isRestore: Boolean = false) {
        if (isRestore) {
            finishProgress()
        } else {
            finishFakeProgress() // 内容返回时完成进度
        }
        mStoryboardListData.value = info
        if (info.shotSegmentList.isNullOrEmpty()) {
            return
        }
        isFirstGenerate.set(info.firstGenStatus == 1)
        totalStoryBoardCount.set(info.shotSegmentList.size)
        if (info.shotSegmentList.size == adapter.items.size) {
            info.shotSegmentList.forEachIndexed { index, segment ->
                adapter.items.getOrNull(index)?.data?.set(segment)
            }
        } else {
            info.shotSegmentList.mapIndexed { index, segment ->
                ItemStoryboardVM(segment).apply {
                    onItemClick.value = {
                        showStoryboardEditor?.invoke(index, this)
                    }
                }
            }.let { adapter.submitList(it) }
        }
        inputAllComplete.set(!adapter.items.any { it.isInputNotComplete() })
        nextStepEnabled.set(info.genStatus == 2)
    }

    /**
     * 计算价格
     * @param onSuccess 成功回调，返回点数金额
     */
    fun calculatePrice(onSuccess: ((Int?) -> Unit)? = null) {
        launchWithLoading {
            response(mRepo.calculatePrice(dramaId.value)) {
                onSuccess?.invoke(data?.pointAmount)
            }
        }
    }

    /**
     * 确认生成
     * @param onSuccess 成功回调，返回剧本ID
     */
    fun confirmGenerate(onSuccess: ((String?) -> Unit)? = null) {
        launchWithLoading {
            response(mRepo.shotDramaGenerate(dramaId.value)) {
                onSuccess?.invoke(data?.dramasId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseJob()
        loopRoleListJob?.cancel()
        initLoadingJon?.cancel()
        loopGeneratingSegmentsJob?.cancel()
        fakeProgressJob?.cancel() // 取消假进度任务
    }

}