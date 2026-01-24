package com.kgmedia.fastfilm.feature.mine.drafts

import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2025/9/11/周四  17:25
 * @Version: 1.0
 * *****************************************
 */
sealed class ShortDramaDraftEvent {


    //查看
    class Complete(val shortDramaInfo: ShortDramaInfo?) : ShortDramaDraftEvent()
    class ItemClick(val shortDramaInfo: ShortDramaInfo?) : ShortDramaDraftEvent()
}