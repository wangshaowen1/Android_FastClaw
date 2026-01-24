package com.kgmedia.fastfilm.feature.creator.character

import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.item.BaseItemEventsViewModel

/**
 * *****************************************
 * 项目名称: KuaigeAi
 * @Author: SZYMN
 * @Introduce:制作绘本-主角形象
 * @Data: 2025/9/5/周五  15:15
 * @Version: 1.0
 * *****************************************
 */
class ItemCharacterVm(
    private val eventVm: BaseItemEventsViewModel<CharacterEvent, String>,
    character: CharacterInfo?,
) : AppItemViewModel<CharacterInfo>(character) {

    fun regenerate() {
        eventVm.postEvent(CharacterEvent.Regenerate(data.get()))
    }

    fun delRole() {
        eventVm.postEvent(CharacterEvent.DelRole(data.get()))
    }

    fun historyRoles() {
        if (data.get()?.genStatus == 3) {
            "当前图片生成失败".showToast()
            return
        }
        if (data.get()?.genStatus == 2) {
            eventVm.postEvent(CharacterEvent.HistoryRoles(data.get()))
        }
    }
}