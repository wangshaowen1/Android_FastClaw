package com.kgmedia.fastfilm.feature.mine.drafts

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.common.utils.DateUtil
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.nhsd.basic.item.ItemEventsViewModel

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:绘本作品
 * @Data: 2025/4/29  10:17
 * @Version: 1.0
 * *****************************************
 */
class ItemShortDramaInfoWorksVm(
    private val eventVm: ItemEventsViewModel<ShortDramaDraftEvent>,
    shortDramaInfo: ShortDramaInfo?
) : AppItemViewModel<ShortDramaInfo>(shortDramaInfo) {

    fun complete() {
        eventVm.postEvent(ShortDramaDraftEvent.Complete(data.get()))
    }

    fun onItemClick() {
        eventVm.postEvent(ShortDramaDraftEvent.ItemClick(data.get()))
    }

    fun title(): String {
        return data.get()?.dramasName ?: ""
    }

    fun time(): String {
        return "${DateUtil.format(data.get()?.createTime ?: 0L, DateUtil.FORMAT_DATETIME_BIAS)} 创作"
    }

    fun statusText(): String? {
        return when (data.get()?.workStatus) {
            ShortDramaInfo.STATUS_WAITING, ShortDramaInfo.STATUS_GENERATING -> "画面生成中"
            ShortDramaInfo.STATUS_GENERATED_FAILED -> "画面生成失败"
            ShortDramaInfo.STATUS_GENERATED_PART_FAILED -> "画面部分失败"
            ShortDramaInfo.STATUS_WAIT_FOR_CHARACTER_CREATION -> "选择角色，生成画面"
            ShortDramaInfo.STATUS_PENDING_VIDEO_GENERATION -> "去合成小说"
            else -> {
                "发布作品到小说"
            }
        }
    }

    fun isFailed(status: Int): Boolean {
        return status == ShortDramaInfo.STATUS_GENERATED_FAILED || status == ShortDramaInfo.STATUS_GENERATED_PART_FAILED
    }

    fun isSuccess(status: Int): Boolean {
        return status == ShortDramaInfo.STATUS_GENERATED_SUCCESS
    }

}