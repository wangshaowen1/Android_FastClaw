/*
 * Copyright (C) 2009 Teleca Poland Sp. z o.o. <android@teleca.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kgmedia.fastfilm.kit

import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo


/**
 * Listener interface for PlayerEngine events.
 *
 */
interface PlaylistListener {
    /**
     * Enumeration representing the playback status of the PlayerEngine.
     */
    enum class Status {
        BUFFERING,  // The engine is buffering data.
        STARTED,  // Playback has started.
        PAUSED,  // Playback is paused.
        STOPPED // Playback has stopped.
    }

    /**
     * Callback method called when the playlist has changed.
     *
     * @param shorts The new list of shorts in the playlist.
     */
    fun onPlaylistChanged(shorts: List<ShortDramaInfo?>?) {
        // Default implementation is empty.
    }

    /**
     * Callback method called before a new short starts playing.
     * Returning false prevents the short from playing.
     *
     * @param short The information about the short that is about to start.
     * @return true to allow the short to play, false to prevent it.
     */
    fun onShortStart(short: ShortDramaInfo?): Boolean {
        return true
    }

    /**
     * Callback method called when a new short begins playing.
     *
     * @param short The information about the short that has started playing.
     */
    fun onShortChanged(short: ShortDramaInfo?) {
        // Default implementation is empty.
    }

    /**
     * Callback method called periodically to update the short's playback progress.
     *
     * @param milliseconds The current playback position in milliseconds.
     */
    fun onShortProgress(milliseconds: Long) {
        // Default implementation is empty.
    }

    /**
     * Callback method called when the playing status of the PlayerEngine changes.
     *
     * @param status The new status of the PlayerEngine.
     */
    fun onPlayingStatusChange(status: Status?) {
        // Default implementation is empty.
    }

    /**
     * Callback method called when an error occurs with the short stream.
     */
    fun onShortStreamError() {
        // Default implementation is empty.
    }
}
