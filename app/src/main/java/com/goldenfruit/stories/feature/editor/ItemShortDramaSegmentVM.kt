package com.kgmedia.fastfilm.feature.editor

import androidx.databinding.ObservableBoolean
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentInfo

/**
 * 短剧编辑-片段
 */
class ItemShortDramaSegmentVM(data: ShortDramaSegmentInfo?, val index: Int, val firstGenLocked: ObservableBoolean, val allSegmentsGenerated: ObservableBoolean) : AppItemViewModel<ShortDramaSegmentInfo>(data) {
    val selected by lazy { ObservableBoolean(false) }

    /**
     * 生成状态 [0=排队中/待生成 1=生成中 2=生成成功 3=生成失败]
     */
    fun genStatus(status: Int?) =
        when (status) {
            0, 1 -> "画面生成中\n请耐心等待..."
            3 -> "画面生成失败\n点击重试"
            else -> null
        }

    /**
     * 生成状态 [0=排队中/待生成 1=生成中 2=生成成功 3=生成失败]
     */
    fun genStatusSimple(status: Int?) =
        when (status) {
            0, 1 -> "生成中..."
            3 -> "生成失败"
            else -> null
        }

    /**
     * [枚举]生成状态 [0=排队中/待生成 1=生成中 2=生成成功 3=生成失败]
     */
    fun genStatusBig(status: Int?) =
        when (status) {
            0, 1 -> "图片生成中"
            3 -> "图片生成失败"
            else -> null
        }

    val isLocked get() = index > 2 && firstGenLocked.get() && allSegmentsGenerated.get()
}