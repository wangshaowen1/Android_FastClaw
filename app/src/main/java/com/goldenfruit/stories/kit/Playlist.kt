package com.kgmedia.fastfilm.kit

import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo
import com.kgmedia.fastfilm.kit.PlaylistPlayer.PlayMode

/** Playlist class that holds a mutable list of ShortDramaInfo objects*/
class Playlist(val playList: MutableList<ShortDramaInfo>) {
    // Private variable to keep short of the current short index
    private var currentShortIndex = 0

    // Returns the current short or null if the index is out of bounds
    fun current(): ShortDramaInfo? {
        return playList.getOrNull(currentShortIndex)
    }

    // Skips to a specific short by index and returns the selected short or null
    fun skipTo(index: Int): ShortDramaInfo? {
        currentShortIndex = index
        return playList.getOrNull(currentShortIndex)
    }

    // Moves to the next short in the playlist and returns the next short or null
    fun next(): ShortDramaInfo? {
        if (playList.isEmpty()) return null
        currentShortIndex = (currentShortIndex + 1) % playList.size
        return playList.getOrNull(currentShortIndex)
    }

    // Moves to the previous short in the playlist and returns the previous short or null
    fun prev(): ShortDramaInfo? {
        val size = playList.size
        if (size == 0) {
            return null
        }
        currentShortIndex = (currentShortIndex - 1 + size) % size
        return playList.getOrNull(currentShortIndex)
    }

    // Checks if there is a next short based on the play mode
    fun hasNext(playMode: PlayMode?): Boolean {
        when (playMode) {
            PlayMode.PLAY_LIST_LOOP -> {
                // Always has a next short in playlist loop mode
                return true
            }

            PlayMode.NORMAL -> {
                // Has a next short if the current index is less than the playlist size minus one
                return (currentShortIndex + 1 < playList.size)
            }

            PlayMode.SHUFFLE -> {
                // Always has a next short in shuffle mode
                return true
            }

            PlayMode.SINGLE_LOOP -> {
                // Always has a next short in single short loop mode
                return true
            }

            null -> {
                // No play mode specified, so no next short
                return false
            }
        }
    }

    // Automatically selects the next short based on the play mode
    fun autoNext(playMode: PlayMode?): ShortDramaInfo? {
        var next: ShortDramaInfo? = null
        when (playMode) {
            PlayMode.PLAY_LIST_LOOP -> {
                // Moves to the next short in the playlist
                next = next()
            }

            PlayMode.NORMAL -> {
                // Moves to the next short if it exists
                if (++currentShortIndex < playList.size) {
                    next = playList.getOrNull(currentShortIndex)
                }
            }

            PlayMode.SHUFFLE -> {
                // Selects a random short and updates the current index
                next = playList.random()
                currentShortIndex = playList.indexOfFirst { item->item.dramasId==next.dramasId }
            }

            PlayMode.SINGLE_LOOP -> {
                // Repeats the current short
                next = playList.getOrNull(currentShortIndex)
            }

            null -> {
                // No play mode specified, so no next short
                next = null
            }
        }
        return next
    }

    // Automatically selects the previous short based on the play mode
    fun autoPrev(playMode: PlayMode): ShortDramaInfo? {
        var next: ShortDramaInfo? = null
        when (playMode) {
            PlayMode.PLAY_LIST_LOOP -> {
                // Moves to the previous short in the playlist
                next = prev()
            }

            PlayMode.NORMAL -> {
                // Moves to the previous short if it exists
                if (--currentShortIndex >= 0) {
                    next = playList.getOrNull(currentShortIndex)
                }
            }

            PlayMode.SHUFFLE -> {
                // Selects a random short and updates the current index
                next = playList.random()
                currentShortIndex = playList.indexOfFirst { item->item.dramasId==next.dramasId }
            }

            PlayMode.SINGLE_LOOP -> {
                // Repeats the current short
                next = playList.getOrNull(currentShortIndex)
            }
        }
        return next
    }

   fun getCurrentShortIndex():Int{
        return currentShortIndex
    }
}