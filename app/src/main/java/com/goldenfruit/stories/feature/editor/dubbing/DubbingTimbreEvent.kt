package com.kgmedia.fastfilm.feature.editor.dubbing

sealed class DubbingTimbreEvent {
    class Click(val item: ItemDubbingTimbreVM) : DubbingTimbreEvent()
}