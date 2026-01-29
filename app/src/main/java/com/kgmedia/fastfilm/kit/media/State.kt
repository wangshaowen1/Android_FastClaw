package com.kgmedia.fastfilm.kit.media

import com.kgmedia.fastfilm.core.network.apis.entity.DramaInfo
import com.kgmedia.fastfilm.kit.media.source.TracksPageSource

/** State class to hold the information about the current track and playback position.*/
class State() {

    var trackList:List<DramaInfo>?=null
    // Variable to store the track information, nullable to accommodate the absence of a track.
    var dramaInfo: DramaInfo? = null

    // Variable to store the current playback position, nullable to represent the absence of a position.
    var playbackPosition: Long? = 0
    var mediaSource: TracksPageSource? = null
    /**
     * Secondary constructor to initialize the State with specific track information and position.
     */
    constructor(dramaInfo: DramaInfo?, position: Long?) : this() {
        this.dramaInfo = dramaInfo
        this.playbackPosition = position
    }

    /**
     * Function to update the state by peeking at the current state of the player.
     */
    fun peek(player: TrackPlayer) {
        dramaInfo = player.currentTrack()
        playbackPosition = player.currentPosition
    }

    /**
     * Function to clear the state, setting track information and position to null.
     */
    fun clear() {
        dramaInfo = null
        playbackPosition = null
        trackList=null
        mediaSource=null
    }

    fun isAvailable(): Boolean {
        return dramaInfo!=null
    }
}