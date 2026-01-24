package com.kgmedia.fastfilm.kit // 建议放在 Kit 层的某个包下

import com.kgmedia.fastfilm.core.manager.EventManager
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.nhsd.basic.ext.launchNoCrash

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.let

/**
 * 负责监听播放器状态并桥接到 EventManager 进行上报
 */
object PlayerAnalyticsMediator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    fun registerPlayListener() {

        val listener = object : PlaylistListener {

            override fun onShortChanged(short: ShortDramaInfo?) {
                super.onShortChanged(short)
                handleOnDramaChanged(short)
            }

        }
        globalShortsPlayer.addListener(listener)

    }

    fun handleOnDramaChanged(shortInfo: ShortDramaInfo?) {
        shortInfo?.let {
            scope.launchNoCrash {
                EventManager.onDramaShortStart(
                    it.dramasId)
            }

        }

    }



}