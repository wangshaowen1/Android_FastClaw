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

package com.kgmedia.fastfilm.kit;

import androidx.annotation.Nullable;

import com.kgmedia.fastfilm.core.network.apis.entity.ShortDramaInfo;

import java.util.List;

/**
 * Interface to the player list, providing methods to control playback and manage playlists.
 */
public interface PlaylistPlayer {

    /**
     * Enum representing the different play modes available for the playlist player.
     */
    enum PlayMode {
        /**
         * Normal playback mode, plays shorts in the order they are in the playlist.
         */
        NORMAL,
        /**
         * Shuffle playback mode, plays shorts in a random order.
         */
        SHUFFLE,
        /**
         * Play list loop mode, loops the entire playlist after it finishes.
         */
        PLAY_LIST_LOOP,
        /**
         * Single short loop mode, loops the currently playing short.
         */
        SINGLE_LOOP
    }

    /**
     * Sets the playback mode for the playlist player.
     *
     * @param playMode The play mode to set.
     */
    void setPlayMode(PlayMode playMode);

    /**
     * Retrieves the current playback mode of the playlist player.
     *
     * @return The current play mode.
     */
    PlayMode getPlayMode();

    /**
     * Sets the playlist to be played.
     *
     * @param playlist The list of shorts to set as the playlist.
     */
    void setPlaylist(List<ShortDramaInfo> playlist);

    void addToPlaylist(ShortDramaInfo shortInfo);

    void addToPlaylist(List<ShortDramaInfo> playlist);

    /**
     * Retrieves the current playlist.
     *
     * @return The current playlist.
     */
    List<ShortDramaInfo> getPlaylist();

    /**
     * Starts playing the current short in the playlist.
     */
    void play();

    /**
     * Retrieves the short that is currently playing.
     *
     * @return The currently playing short, or null if no short is playing.
     */
    @Nullable
    ShortDramaInfo currentShort();
    /**
     * Gets the index of the short that is currently playing in the playlist.
     *
     * @return The index of the currently playing short, or -1 if no short is playing or the index cannot be determined.
     */
    int getCurrentPlayingIndex();

    /**
     * Retrieves the current playback position in milliseconds.
     *
     * @return The current playback position in milliseconds.
     */
    long playbackPosition();

    /**
     * Checks whether the player is currently playing a short.
     *
     * @return true if the player is playing, false otherwise.
     */
    boolean isPlaying();

    /**
     * Stops the playback.
     */
    void stop();

    /**
     * Releases the player resources.
     */
    void release();

    /**
     * Pauses the playback of the current short.
     */
    public void pause();

    /**
     * Moves to the next short in the playlist.
     *
     * @return true if the player successfully moved to the next short, false otherwise.
     */
    boolean next();

    /**
     * Moves to the previous short in the playlist.
     *
     * @return true if the player successfully moved to the previous short, false otherwise.
     */
    boolean prev();

    /**
     * Jumps to the last playlist.
     *
     * @return true if the player successfully jumped to the last playlist, false otherwise.
     */
    boolean prevList();

    /**
     * Skips to a specific short in the playlist.
     *
     * @param index The index of the short in the playlist to skip to.
     */
    void skipTo(int index);

    /**
     * Adds an event listener to the player engine.
     *
     * @param playerListListener The listener to add for player events.
     */
    void addListener(PlaylistListener playerListListener);

    /**
     * Removes an event listener from the player engine.
     *
     * @param playerListListener The listener to remove for player events.
     */
    void removeListener(PlaylistListener playerListListener);

    /**
     * Fast forwards the current short by the specified time.
     *
     * @param time The time in milliseconds to fast forward.
     */
    void forward(int time);

    /**
     * Rewinds the current short by the specified time.
     *
     * @param time The time in milliseconds to rewind.
     */
    void rewind(int time);

    /**
     * Seeks to a specific position in the current short.
     *
     * @param positionMs The position in milliseconds to seek to.
     */
    void seekTo(Long positionMs);

    /**
     * Retrieves the duration of the current short in milliseconds.
     *
     * @return The duration of the current short in milliseconds.
     */
    long getDuration();

    /**
     * Checks if the specified short is in the playlist.
     *
     * @param shortInfo The short information to check.
     * @return true if the short is in the playlist, false otherwise.
     */
    boolean inPlayList(ShortDramaInfo shortInfo);
    /**
     * Checks if the entire playlist has finished playback.
     * Returns true if:
     * - The playlist is not empty,
     * - The player is not in a loop mode (PLAY_LIST_LOOP/SINGLE_short_LOOP),
     * - The current short is the last in the playlist,
     * - The playback has reached the end of the last short.
     *
     * @return true if the playlist has ended, false otherwise.
     */
    boolean isPlaylistEnded();
    /**
     * Restarts playback from the beginning of the playlist.
     * Stops the current short, resets the playlist index to 0, and starts playing from the first short.
     * If the playlist is empty, this method does nothing.
     */
    void restart();


}
