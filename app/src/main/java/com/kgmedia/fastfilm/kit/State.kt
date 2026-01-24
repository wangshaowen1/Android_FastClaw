package com.kgmedia.fastfilm.kit

import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.kit.source.ShortsPageSource

/** State class to hold the information about the current short and playback position.*/
class State() {

    var shortList:List<ShortDramaInfo>?=null
    // Variable to store the short information, nullable to accommodate the absence of a short.
    var short: ShortDramaInfo? = null

    // Variable to store the current playback position, nullable to represent the absence of a position.
    var playbackPosition: Long? = 0
    var mediaSource: ShortsPageSource? = null
    /**
     * Secondary constructor to initialize the State with specific short information and position.
     */
    constructor(short: ShortDramaInfo?, position: Long?) : this() {
        this.short = short
        this.playbackPosition = position
    }

    /**
     * Function to update the state by peeking at the current state of the player.
     */
    fun peek(player: ShortPlayer) {
        short = player.currentShort()
        playbackPosition = player.currentPosition
    }
    /**
     * Function to update the state by peeking at the current state of the player.
     */
    fun peek(player: DefaultPlaylistPlayer?) {
        shortList=player?.playlist
        short = player?.currentShort()
        playbackPosition = player?.playbackPosition()
        mediaSource=player?.setupMediaSourceManager()?.mediaSource
    }

    /**
     * Function to clear the state, setting short information and position to null.
     */
    fun clear() {
        short = null
        playbackPosition = null
        shortList=null
        mediaSource=null
    }

    fun isAvailable(): Boolean {
        return short!=null
    }
}