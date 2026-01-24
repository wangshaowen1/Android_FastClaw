package com.kgmedia.fastfilm.feature.creator.character

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kgmedia.fastfilm.base.item.AppItemViewModel
import com.kgmedia.fastfilm.core.network.apis.entity.CharacterInfo
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
class ItemCharacterHistoryVm(
    character: CharacterInfo?,
) : AppItemViewModel<CharacterInfo>(character) {

    val index = ObservableField<String>()
    val sel = ObservableBoolean(false)
}