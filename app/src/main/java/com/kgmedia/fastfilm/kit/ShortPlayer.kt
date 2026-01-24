package com.kgmedia.fastfilm.kit

import androidx.media3.common.Player
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentInfo
import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaSegmentSubtitleInfo

/**
 * Defines a ShortPlayer interface that extends the Player interface.
 * The ShortPlayer interface provides functionality for managing short playback and listening for short changes.
 */
interface ShortPlayer: Player {

    /**
     * The OnShortChangeListener interface defines the method that should be called when a short change occurs.
     * Classes implementing this interface can be notified when a short change happens.
     */
    interface OnShortChangeListener {
        /**
         * This method is called when the short changes.
         * @param lastState The state of the player before the change.
         * @param short The information of the short after the change.
         */
        fun onShortChanged(lastState: State?, short: ShortDramaInfo?)
        /**
         * This method is called when the short playback changes.
         * @param short The information of the short after the change.
         * @param segment The information of the segment after the change.
         * @param subSegment The information of the subsegment after the change.
         */
        fun onShortPlaybackChange(short: ShortDramaInfo?, segment: ShortDramaSegmentInfo?, subSegment: ShortDramaSegmentSubtitleInfo?){}
    }
    data class GlobalPlaybackInfo(
        val positionMs: Long,
        val durationMs: Long,
        val mediaItemIndex: Int
    )

    fun getGlobalPlaybackInfo(): GlobalPlaybackInfo

    fun seekToGlobalPosition(positionMs: Long)

    /**
     * Adds a short change listener to be notified when a short change occurs.
     * @param listener The short change listener to be added.
     */
    fun addOnShortChangeListener(listener: OnShortChangeListener?)

    /**
     * Removes a short change listener so it no longer receives notifications when a short changes.
     * @param listener The short change listener to be removed.
     */
    fun removeOnShortChangeListener(listener: OnShortChangeListener?)

    /**
     * Sets the short information to be played.
     * @param short The short information to set for the current playback.05
     */
    fun setShort(short: ShortDramaInfo?)

    /**
     * Gets the information of the currently playing short.
     * @return The information of the currently playing short, or null if no short is playing.
     */
    fun currentShort(): ShortDramaInfo?
}