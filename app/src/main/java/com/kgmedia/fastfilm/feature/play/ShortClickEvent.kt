package com.kgmedia.fastfilm.feature.play

import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo

interface ShortClickEvent {
    class ItemClick(val short: ShortDramaInfo?,val position: Int) :ShortClickEvent
}