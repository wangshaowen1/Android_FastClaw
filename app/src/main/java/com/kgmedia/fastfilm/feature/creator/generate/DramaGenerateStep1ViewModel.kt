package com.kgmedia.fastfilm.feature.creator.generate

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.ext.launchWithLoading
import com.kgmedia.fastfilm.core.network.SSEStreamHandler
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.kgmedia.fastfilm.core.network.apis.request.DramaGenerateBody
import com.kgmedia.fastfilm.core.network.apis.request.InputContentBody
import com.kgmedia.fastfilm.core.network.apis.request.PopularRecommendDramasIdBody
import com.kgmedia.fastfilm.core.response
import com.kgmedia.fastfilm.feature.creator.data.CreationRepository
import com.nhsd.basic.utils.Logger

/**
 * 故事板视图模型，负责处理故事板生成相关的业务逻辑
 * 包括SSE内容生成、角色列表获取、分镜列表获取等功能
 */
class DramaGenerateStep1ViewModel : AppRepositoryViewModel<CreationRepository>() {

    companion object {
        const val TAG = "DramaGenerateStep1ViewModel"
        const val DISPLAY_LOADING = 0
        const val DISPLAY_FAIL = 1
        const val DISPLAY_CONTENT = 2
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
    val nextStepEnabled by lazy { ObservableBoolean(false) }

    // 定义为成员变量
    private var sseHandler: SSEStreamHandler? = null

    fun sseGenerate() {
        // 1. 准备请求 Flow
        val sseFlow = if (model == DramaGenerateBody.GEN_WAY_MAKE_SAME_STORY) {
            mRepo.api.sseMakeSameContentGenerate(PopularRecommendDramasIdBody(otherExtraId))
        } else {
            mRepo.api.sseContentGenerate(InputContentBody(model, content))
        }

        // 2. 初始化处理器 (如果还没初始化，或者想每次重建也可以)
        sseHandler?.cancel()
        sseHandler = SSEStreamHandler(
            scope = viewModelScope, // 传入协程作用域
            onStart = {
                displayChild.set(DISPLAY_LOADING)
                mSSEContent.set("")
                mContent.set("")
                Logger.d("[SSE]", "Open")
            },
            onContentUpdate = { fullContent ->
                // 这里自动包含了 50ms 的节流逻辑，直接设置即可
                mSSEContent.set(fullContent)
            },
            onComplete = {
                Logger.d("[SSE]", "Finished")
                // 触发原本结束后的逻辑
            },
            onError = { e ->
                Log.e("[SSE]", "Error: $e")
                displayChild.set(DISPLAY_FAIL)
                // 根据需要处理特定错误
            }
        )

        // 3. 启动
        Logger.d("[SSE]", "Starting with content body = $content")
        sseHandler?.start(sseFlow)
    }

    /**
     * 页面销毁或需要重置时调用
     */
    override fun onCleared() {
        super.onCleared()
        sseHandler?.cancel()
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
}