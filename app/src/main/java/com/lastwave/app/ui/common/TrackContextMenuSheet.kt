package com.lastwave.app.ui.common

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.favorite.FavoritesRepository
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.ui.generate.MixLauncher
import com.lastwave.app.ui.navigation.ArtistAlbumNavigator
import com.lastwave.app.ui.player.LocalAddToPlaylist
import com.lastwave.app.ui.player.LocalMusicPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

sealed interface TrackMenuTarget {
    val name: String
    val artist: String
    val url: String

    data class Track(override val name: String, override val artist: String, override val url: String) : TrackMenuTarget
    data class Artist(override val name: String, override val url: String = "") : TrackMenuTarget {
        override val artist: String get() = ""
    }
    data class Album(override val name: String, override val artist: String, override val url: String = "") : TrackMenuTarget
}

data class TrackMenuCapabilities(
    val showCopyActions: Boolean = true,
    val showDeleteScrobble: Boolean = false,
)

@HiltViewModel
class ArtistAlbumMenuViewModel @Inject constructor(
    private val navigator: ArtistAlbumNavigator,
) : ViewModel() {
    fun openArtist(name: String, browseId: String? = null) {
        navigator.openArtist(name, browseId)
    }

    fun openAlbum(title: String, artist: String = "", browseId: String? = null) {
        navigator.openAlbum(title, artist, browseId)
    }
}

@HiltViewModel
class StartMixMenuViewModel @Inject constructor(private val mixLauncher: MixLauncher) : ViewModel() {
    fun startMix(trackName: String, artistName: String) {
        mixLauncher.startMix(trackName, artistName)
    }
}

@HiltViewModel
class DownloadMenuViewModel @Inject constructor(
    private val downloadManager: com.lastwave.app.data.download.TrackDownloadManager,
) : ViewModel() {
    fun download(title: String, artist: String, album: String? = null, artworkUrl: String? = null) {
        downloadManager.downloadTrack(title, artist, album, artworkUrl)
    }
}

@HiltViewModel
class RecommendationExclusionMenuViewModel @Inject constructor(
    private val discoverRepository: com.lastwave.app.data.discover.DiscoverRepository,
) : ViewModel() {
    fun exclude(trackName: String, artistName: String) {
        viewModelScope.launch {
            discoverRepository.excludeFromRecommendations(trackName, artistName)
        }
    }
}

@HiltViewModel
class ExploreGenreMenuViewModel @Inject constructor(private val genreExplorer: com.lastwave.app.ui.genres.GenreExplorer) : ViewModel() {
    fun explore(genre: String) {
        genreExplorer.explore(genre)
    }
}

@HiltViewModel
class FavoritesMenuViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {
    fun isFavorite(title: String, artist: String): Flow<Boolean> =
        favoritesRepository.isFavorite(title, artist)

    fun toggleFavorite(track: PlayableTrack, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val isNowFav = favoritesRepository.toggleFavorite(track)
            onResult(isNowFav)
        }
    }

    fun syncLikedSongs(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = favoritesRepository.syncLikedSongsToYouTube()
            onResult(success)
        }
    }
}

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val targetUri = if (url.startsWith("http://") || url.startsWith("https://")) {
            android.net.Uri.parse(url)
        } else {
            android.net.Uri.parse("https://$url")
        }
        context.startActivity(Intent(Intent.ACTION_VIEW, targetUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // Handle gracefully
    }
}

private fun buildLastFmUrl(target: TrackMenuTarget): String {
    return when (target) {
        is TrackMenuTarget.Track -> if (target.url.isNotBlank()) target.url else try {
            "https://www.last.fm/music/${java.net.URLEncoder.encode(target.artist, "UTF-8")}/_/${java.net.URLEncoder.encode(target.name, "UTF-8")}"
        } catch (e: Exception) { "" }
        is TrackMenuTarget.Artist -> if (target.url.isNotBlank()) target.url else try {
            "https://www.last.fm/music/${java.net.URLEncoder.encode(target.name, "UTF-8")}"
        } catch (e: Exception) { "" }
        is TrackMenuTarget.Album -> if (target.url.isNotBlank()) target.url else try {
            "https://www.last.fm/music/${java.net.URLEncoder.encode(target.artist, "UTF-8")}/${java.net.URLEncoder.encode(target.name, "UTF-8")}"
        } catch (e: Exception) { "" }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackContextMenuSheet(
    target: TrackMenuTarget,
    capabilities: TrackMenuCapabilities,
    playableTrack: PlayableTrack? = null,
    onDismiss: () -> Unit,
    playbackSourceLabel: String = "LastWave",
    onPlayInLastWave: (() -> Unit)? = null,
    onStartMix: ((trackName: String, artistName: String) -> Unit)? = null,
    onExploreGenre: ((genre: String) -> Unit)? = null,
    onDeleteScrobble: ((trackName: String, artistName: String) -> Unit)? = null,
    onRefreshArtwork: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    genreResolverViewModel: GenreRowViewModel = hiltViewModel(),
    startMixViewModel: StartMixMenuViewModel = hiltViewModel(),
    exploreGenreViewModel: ExploreGenreMenuViewModel = hiltViewModel(),
    downloadViewModel: DownloadMenuViewModel = hiltViewModel(),
    exclusionViewModel: RecommendationExclusionMenuViewModel = hiltViewModel(),
    artistAlbumViewModel: ArtistAlbumMenuViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesMenuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val musicPlayer = LocalMusicPlayer.current
    val addToPlaylist = LocalAddToPlaylist.current

    var showDetailsSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var resolvedGenre by remember(target) { mutableStateOf<String?>(null) }
    var resolvingGenre by remember(target) { mutableStateOf(false) }

    val isFavorite by favoritesViewModel.isFavorite(target.name, target.artist).collectAsStateWithLifecycle(initialValue = false)
    val playerState by musicPlayer.state.collectAsStateWithLifecycle()

    LaunchedEffect(target) {
        if (target is TrackMenuTarget.Track) {
            resolvingGenre = true
            resolvedGenre = runCatching { genreResolverViewModel.resolve(target.name, target.artist) }.getOrNull()
            resolvingGenre = false
        }
    }

    fun exploreGenre(genre: String) {
        if (onExploreGenre != null) onExploreGenre(genre)
        else exploreGenreViewModel.explore(genre)
    }

    if (showDetailsSheet && target is TrackMenuTarget.Track) {
        val playable = playableTrack ?: PlayableTrack(title = target.name, artist = target.artist)
        TrackDetailsSheet(
            title = target.name,
            artist = target.artist,
            album = playable.album,
            artworkUrl = playable.artworkUrl,
            onDismiss = {
                showDetailsSheet = false
                onDismiss()
            },
            onPlayTrack = {
                onPlayInLastWave?.invoke() ?: musicPlayer.play(playable, sourceLabel = playbackSourceLabel)
            },
        )
        return
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            musicPlayer = musicPlayer,
            onDismiss = { showSleepTimerSheet = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp),
            ) {}
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp + safeDrawingBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val playable = playableTrack ?: PlayableTrack(title = target.name, artist = target.artist)

            // ── Hero Header Card ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkImage(
                    name = target.name,
                    artist = if (target is TrackMenuTarget.Track) target.artist else "",
                    embeddedUrl = playable.artworkUrl,
                    fallbackIcon = Icons.Filled.MusicNote,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        target.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (target is TrackMenuTarget.Track && target.artist.isNotBlank()) target.artist
                        else if (target is TrackMenuTarget.Album && target.artist.isNotBlank()) target.artist
                        else "Artist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!resolvedGenre.isNullOrBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            onClick = { exploreGenre(resolvedGenre!!); onDismiss() },
                        ) {
                            Text(
                                resolvedGenre!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // ── Quick 4-Action Matrix Bar (Hero Pills) ──────────────────
            if (target is TrackMenuTarget.Track) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 1. Favorite / Like Button
                    QuickActionPill(
                        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        iconTint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        containerColor = if (isFavorite) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        label = if (isFavorite) "Liked" else "Favorite",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            favoritesViewModel.toggleFavorite(playable) { nowFav ->
                                Toast.makeText(context, if (nowFav) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )

                    // 2. Sleep Timer Button
                    val remainingMs = playerState.sleepTimerRemainingMs
                    val sleepTimerLabel = if (remainingMs != null && remainingMs > 0L) {
                        "${(remainingMs / 60_000L).coerceAtLeast(1)}m left"
                    } else {
                        "Timer"
                    }
                    QuickActionPill(
                        icon = Icons.Filled.Timer,
                        iconTint = if (remainingMs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        containerColor = if (remainingMs != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        label = sleepTimerLabel,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSleepTimerSheet = true
                        },
                    )

                    // 3. Download Button
                    QuickActionPill(
                        icon = Icons.Filled.Download,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        label = "Download",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            downloadViewModel.download(target.name, target.artist, playable.album, playable.artworkUrl)
                            Toast.makeText(context, "Downloading track...", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                    )

                    // 4. Start Radio / Mix Button
                    QuickActionPill(
                        icon = Icons.Filled.AutoAwesome,
                        iconTint = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        label = "Start Mix",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (onStartMix != null) onStartMix(target.name, target.artist)
                            else startMixViewModel.startMix(target.name, target.artist)
                            onDismiss()
                        },
                    )
                }
            }

            // ── Primary Actions Group ────────────────────────────────
            if (target is TrackMenuTarget.Track) {
                val splitArtists = com.lastwave.app.util.ArtistHelper.splitArtists(target.artist)
                val primaryArt = splitArtists.firstOrNull() ?: target.artist

                // 1. Playback & Queue Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val queueRows = buildList<@Composable (GroupPosition) -> Unit> {
                        add { pos ->
                            SleekActionRow(
                                icon = Icons.Filled.PlayCircle,
                                title = "Play in LastWave",
                                subtitle = "Start playback immediately",
                                position = pos,
                            ) {
                                onPlayInLastWave?.invoke() ?: musicPlayer.play(playable, sourceLabel = playbackSourceLabel)
                                onDismiss()
                            }
                        }
                        add { pos ->
                            SleekActionRow(
                                icon = Icons.Filled.QueuePlayNext,
                                title = "Play Next",
                                subtitle = "Insert right after current song",
                                position = pos,
                            ) {
                                musicPlayer.playNext(playable)
                                Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                        add { pos ->
                            SleekActionRow(
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                title = "Add to Queue",
                                subtitle = "Append to end of queue",
                                position = pos,
                            ) {
                                musicPlayer.addToQueue(playable)
                                Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                        add { pos ->
                            SleekActionRow(
                                icon = Icons.Filled.PlaylistAdd,
                                title = "Add to Playlist",
                                subtitle = "Save into custom playlists",
                                position = pos,
                            ) {
                                addToPlaylist(playable)
                                onDismiss()
                            }
                        }
                    }
                    ExpressiveGroup(rowCount = queueRows.size) { index, position -> queueRows[index](position) }
                }

                // 2. Navigation & Discovery Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val navRows = buildList<@Composable (GroupPosition) -> Unit> {
                        for (art in splitArtists) {
                            add { pos ->
                                SleekActionRow(
                                    icon = Icons.Filled.Person,
                                    title = "Go to Artist",
                                    subtitle = art,
                                    position = pos,
                                ) {
                                    artistAlbumViewModel.openArtist(art)
                                    onDismiss()
                                }
                            }
                        }
                        if (!playable.album.isNullOrBlank()) {
                            add { pos ->
                                SleekActionRow(
                                    icon = Icons.Filled.Album,
                                    title = "Go to Album",
                                    subtitle = playable.album,
                                    position = pos,
                                ) {
                                    artistAlbumViewModel.openAlbum(playable.album!!, primaryArt)
                                    onDismiss()
                                }
                            }
                        }
                        add { pos ->
                            SleekActionRow(
                                icon = Icons.Filled.Info,
                                title = "Details & Audio Specs",
                                subtitle = "Lossless bit-depth, codec & metadata",
                                position = pos,
                            ) {
                                showDetailsSheet = true
                            }
                        }
                    }
                    ExpressiveGroup(rowCount = navRows.size) { index, position -> navRows[index](position) }
                }

                // 3. YouTube Music Sync & External Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val syncRows = buildList<@Composable (GroupPosition) -> Unit> {
                        add { pos ->
                            SleekActionRow(
                                icon = Icons.Filled.CloudSync,
                                title = "Sync Liked Songs to YouTube Music",
                                subtitle = "Mirror your local favorites to YouTube Music",
                                position = pos,
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, "Syncing favorites to YouTube Music...", Toast.LENGTH_SHORT).show()
                                favoritesViewModel.syncLikedSongs { success ->
                                    Toast.makeText(context, if (success) "YouTube Music favorites synced!" else "Sync completed", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        }
                        if (capabilities.showCopyActions) {
                            add { pos ->
                                SleekActionRow(
                                    icon = Icons.Filled.ContentCopy,
                                    title = "Copy Song Details",
                                    subtitle = "${target.name} — ${target.artist}",
                                    position = pos,
                                ) {
                                    clipboard.setText(AnnotatedString("${target.name} — ${target.artist}"))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        }
                        add { pos ->
                            SleekActionRow(
                                icon = Icons.Filled.Language,
                                title = "Open in Last.fm",
                                subtitle = "View global scrobbles and biography",
                                position = pos,
                            ) {
                                openUrl(context, buildLastFmUrl(target))
                                onDismiss()
                            }
                        }
                        if (onRefreshArtwork != null) {
                            add { pos ->
                                SleekActionRow(
                                    icon = Icons.Filled.Refresh,
                                    title = "Refresh Cover Art",
                                    subtitle = "Re-fetch high-resolution album cover",
                                    position = pos,
                                ) {
                                    onRefreshArtwork()
                                    onDismiss()
                                }
                            }
                        }
                    }
                    ExpressiveGroup(rowCount = syncRows.size) { index, position -> syncRows[index](position) }
                }

                // 4. Recommendation Exclusion / Manage Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val manageRows = buildList<@Composable (GroupPosition) -> Unit> {
                        add { pos ->
                            SleekActionRow(
                                icon = Icons.Filled.ThumbDown,
                                title = "Don't Recommend Again",
                                subtitle = "Hide from mixes and discovery feeds",
                                danger = true,
                                position = pos,
                            ) {
                                exclusionViewModel.exclude(target.name, target.artist)
                                Toast.makeText(context, "Excluded from recommendations", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                        if (onRemoveFromPlaylist != null) {
                            add { pos ->
                                SleekActionRow(
                                    icon = Icons.Filled.Delete,
                                    title = "Remove from Playlist",
                                    subtitle = "Remove this track from playlist",
                                    danger = true,
                                    position = pos,
                                ) {
                                    onRemoveFromPlaylist()
                                    onDismiss()
                                }
                            }
                        }
                    }
                    ExpressiveGroup(rowCount = manageRows.size) { index, position -> manageRows[index](position) }
                }
            } else if (target is TrackMenuTarget.Artist) {
                val rows = buildList<@Composable (GroupPosition) -> Unit> {
                    add { pos ->
                        SleekActionRow(
                            icon = Icons.Filled.Person,
                            title = "View Artist Page",
                            subtitle = "Top tracks, albums, and similar artists",
                            position = pos,
                        ) {
                            artistAlbumViewModel.openArtist(target.name)
                            onDismiss()
                        }
                    }
                    add { pos ->
                        SleekActionRow(
                            icon = Icons.Filled.Language,
                            title = "Open in Last.fm",
                            subtitle = "Artist bio, tags, and charts",
                            position = pos,
                        ) {
                            openUrl(context, buildLastFmUrl(target))
                            onDismiss()
                        }
                    }
                }
                ExpressiveGroup(rowCount = rows.size) { index, position -> rows[index](position) }
            } else if (target is TrackMenuTarget.Album) {
                val rows = buildList<@Composable (GroupPosition) -> Unit> {
                    add { pos ->
                        SleekActionRow(
                            icon = Icons.Filled.Album,
                            title = "View Album Page",
                            subtitle = "Full tracklist and album release info",
                            position = pos,
                        ) {
                            artistAlbumViewModel.openAlbum(target.name, target.artist)
                            onDismiss()
                        }
                    }
                    if (target.artist.isNotBlank()) {
                        add { pos ->
                            SleekActionRow(
                                icon = Icons.Filled.Person,
                                title = "View Artist (${target.artist})",
                                subtitle = "Artist discography",
                                position = pos,
                            ) {
                                artistAlbumViewModel.openArtist(target.artist)
                                onDismiss()
                            }
                        }
                    }
                    add { pos ->
                        SleekActionRow(
                            icon = Icons.Filled.Language,
                            title = "Open in Last.fm",
                            subtitle = "Album scrobbles and reviews",
                            position = pos,
                        ) {
                            openUrl(context, buildLastFmUrl(target))
                            onDismiss()
                        }
                    }
                }
                ExpressiveGroup(rowCount = rows.size) { index, position -> rows[index](position) }
            }
        }
    }
}

/**
 * 4-Pill Quick Action Matrix Item
 */
@Composable
private fun QuickActionPill(
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pillScale",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        interactionSource = interactionSource,
        modifier = modifier
            .height(72.dp)
            .scale(scale),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Sleek Group Action Row with Icon Squircle Container, Title & Subtitle
 */
@Composable
private fun SleekActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    danger: Boolean = false,
    position: GroupPosition = GroupPosition.SINGLE,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberGroupPressScale(interactionSource)
    val contentColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val badgeColor = if (danger) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerHighest
    val badgeContentColor = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = groupShape(position),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = badgeContentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/**
 * Dedicated Sleep Timer Selection Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerBottomSheet(
    musicPlayer: com.lastwave.app.playback.MusicPlayer,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playerState by musicPlayer.state.collectAsStateWithLifecycle()
    val remainingMs = playerState.sleepTimerRemainingMs

    val options = listOf(
        Pair(0, "Turn Off Timer"),
        Pair(15, "15 Minutes"),
        Pair(30, "30 Minutes"),
        Pair(45, "45 Minutes"),
        Pair(60, "60 Minutes"),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp),
            ) {}
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp + safeDrawingBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Sleep Timer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (remainingMs != null && remainingMs > 0L) {
                            "Playback stops in ${(remainingMs / 60_000L).coerceAtLeast(1)} minutes"
                        } else {
                            "Automatically pause playback after time expires"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (minutes, title) ->
                    val isSelected = if (minutes == 0) remainingMs == null else {
                        remainingMs != null && (remainingMs / 60_000L).toInt() in (minutes - 2)..(minutes + 2)
                    }

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            musicPlayer.setSleepTimer(minutes)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (minutes == 0) Icons.Filled.TimerOff else Icons.Filled.Timer,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
