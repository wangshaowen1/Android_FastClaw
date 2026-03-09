package com.kgmedia.fastfilm.feature.creator.generate

interface OnBackPressedHandler {
    fun handleBackPressed(): Boolean // 返回 true 表示 Fragment 自己处理了逻辑
}