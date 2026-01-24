package com.kgmedia.fastfilm.feature.timbre

import com.kgmedia.fastfilm.base.viewmodel.AppRepositoryViewModel
import com.kgmedia.fastfilm.common.Constants
import com.kgmedia.fastfilm.common.utils.NetworkUtil
import com.kgmedia.fastfilm.core.AppRepository
import com.kgmedia.fastfilm.core.ext.launch
import com.kgmedia.fastfilm.core.network.apis.request.PictureBookId
import com.kgmedia.fastfilm.feature.timbre.GenerateWaitingDialog.Companion.TYPE_PICTURE_BOOK_GENERATE
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class GenerateWaitingViewModel : AppRepositoryViewModel<AppRepository>() {

    val type by lazy { mBundle.getInt(Constants.Extra.TYPE) }

    override fun onCreate() {
        super.onCreate()
        when (type) {
            TYPE_PICTURE_BOOK_GENERATE -> {
                val pictureBookId = mBundle.getString(Constants.Extra.DRAMA_ID)
                getPictureBookGenerateStatus(pictureBookId)
            }
        }
    }

    companion object{
        const val LOG_TAG = "GenerateWaitingViewModel"
        private const val MAX_POLLING_COUNT = 300 // 最大轮询次数（约10分钟）
        private const val POLLING_INTERVAL = 2000L // 轮询间隔
    }
    private var pollingJob: Job? = null


    fun getPictureBookGenerateStatus(pictureBookId: String?) {
        pollingJob?.cancel() // 取消之前的轮询

        pollingJob = launch({
            var pollingCount = 0
            var isCompleted = false

            while (pollingCount < MAX_POLLING_COUNT && !isCompleted && isActive) {
                try {
                    val response = mRepo.api.getPictureBookStatus(PictureBookId(pictureBookId))

                    if (response.isSuccess) {
                        val status = response.data?.pictureBookGenStatus ?: 0
                        when (status) {
                            3 -> { // 生成失败
                                "生成失败，请重试".showToast()
                                dismiss()
                                isCompleted = true
                            }
                            1, 2 -> { // 生成成功或生成中但可预览
                                //BookPreviewActivity.start(pictureBookId)
                                dismiss()
                                isCompleted = true
                            }
                            0 -> { // 等待生成
                                // 继续轮询
                            }
                            else -> { // 其他状态
                                Logger.w(LOG_TAG, "未知状态: $status")
                            }
                        }
                    } else {
                        // 网络请求失败，记录日志但继续重试
                        Logger.e(LOG_TAG, "轮询请求失败: ${response.msg}")
                    }
                } catch (e: Exception) {
                    Logger.e(LOG_TAG, "轮询异常: ${e.message}")
                }

                if (!isCompleted) {
                    pollingCount++
                    if (pollingCount < MAX_POLLING_COUNT) {
                        delay(POLLING_INTERVAL)
                    }
                }
            }

            // 检查是否因为超过最大次数而退出
            if (pollingCount >= MAX_POLLING_COUNT) {
                "生成超时，请稍后在我的-草稿中查看".showToast()
                dismiss()
            }
        }, onError = {
            // 这里处理的是协程本身的异常，不是网络请求的异常
            if (NetworkUtil.isNetworkAvailable()) {
                "服务器繁忙，请稍后在我的-草稿中查看".showToast()
            } else {
                "网络连接失败，请稍后在我的-草稿中查看".showToast()
            }
            dismiss()
        })
    }

    // 在页面销毁时取消轮询
    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }
}