package com.kgmedia.fastfilm.feature.creator.character

import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:制作绘本-主角形象
 * @Data: 2025/9/9/周二  17:09
 * @Version: 1.0
 * *****************************************
 */
sealed class CharacterEvent {
    //选择形象
    data class Regenerate(val character: CharacterInfo?):CharacterEvent()

    data class DelRole(val character: CharacterInfo?):CharacterEvent()

    data class HistoryRoles(val character: CharacterInfo?):CharacterEvent()
}