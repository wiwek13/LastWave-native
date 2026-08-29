package com.lastwave.app.data.favorite

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.playlist.PlaylistRepository
import com.lastwave.app.data.ytmusic.YtMusicSyncManager
import com.lastwave.app.playback.PlayableTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val playlistRepository: PlaylistRepository,
    private val ytMusicSyncManager: YtMusicSyncManager,
    private val applicationScope: CoroutineScope,
) {
    private object Keys {
        val FAVORITE_KEYS = stringSetPreferencesKey("favorite_track_keys")
    }

    val favoriteKeys: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[Keys.FAVORITE_KEYS] ?: emptySet()
    }

    fun isFavorite(title: String, artist: String): Flow<Boolean> {
        val key = makeKey(title, artist)
        return favoriteKeys.map { it.contains(key) }
    }

    suspend fun toggleFavorite(track: PlayableTrack): Boolean {
        val key = makeKey(track.title, track.artist)
        var nowFavorited = false

        dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_KEYS] ?: emptySet()
            if (key in current) {
                prefs[Keys.FAVORITE_KEYS] = current - key
                nowFavorited = false
            } else {
                prefs[Keys.FAVORITE_KEYS] = current + key
                nowFavorited = true
            }
        }

        // Keep local "Favorites" custom playlist synchronized
        applicationScope.launch(Dispatchers.IO) {
            runCatching {
                val favPlaylist = playlistRepository.createCustom("Favorites")
                if (nowFavorited) {
                    val genTrack = GeneratedTrack(
                        name = track.title,
                        artist = track.artist,
                        artworkUrl = track.artworkUrl,
                        album = track.album,
                    )
                    playlistRepository.addTrack(favPlaylist.id, genTrack)
                } else {
                    val index = favPlaylist.tracks.indexOfFirst {
                        it.name.equals(track.title, ignoreCase = true) &&
                            it.artist.equals(track.artist, ignoreCase = true)
                    }
                    if (index >= 0) {
                        playlistRepository.removeTrack(favPlaylist.id, index)
                    }
                }
                Unit
            }
        }

        return nowFavorited
    }

    suspend fun syncLikedSongsToYouTube(): Boolean {
        return ytMusicSyncManager.syncNow("favorites_manual_sync")
    }

    companion object {
        fun makeKey(title: String, artist: String): String =
            "${title.trim().lowercase()} • ${artist.trim().lowercase()}"
    }
}
