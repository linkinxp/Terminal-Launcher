package dev.linkinxp.termlauncher.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val MAX_COLS = 40

interface MediaControls { fun previous(); fun togglePlay(); fun next() }

/** The track in the media session that matters right now. */
data class MediaInfo(val pkg: String, val title: String, val artist: String, val playing: Boolean, val controls: MediaControls)

data class SessionSummary(val pkg: String, val playing: Boolean)

/** Which active media session to show: the first that is playing, else the first; null when there is none. */
fun pickSession(sessions: List<SessionSummary>): Int? =
    sessions.indexOfFirst { it.playing }.takeIf { it >= 0 } ?: if (sessions.isEmpty()) null else 0

private fun fit(text: String) = if (text.length <= MAX_COLS) text else text.take(MAX_COLS - 1) + "…"

/** The text lines of the now-playing panel: the title (or `(untitled)`), then the artist when there is one. */
fun nowPlayingLines(info: MediaInfo): List<String> =
    listOfNotNull(fit(info.title.trim().ifEmpty { "(untitled)" }), info.artist.trim().takeIf { it.isNotEmpty() }?.let(::fit))

/** The play button names what pressing it will do. */
fun playLabel(playing: Boolean) = if (playing) "[pause]" else "[play]"

object MediaStore {
    private val _state = MutableStateFlow<MediaInfo?>(null)
    val state: StateFlow<MediaInfo?> = _state.asStateFlow()
    fun publish(info: MediaInfo?) { _state.value = info }
}

/**
 * Follows the active media sessions for the notification listener (Android only shows them to an app that has
 * notification access) and keeps [MediaStore] up to date.
 */
class MediaWatcher(private val context: Context, private val listener: ComponentName) {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private var watched: MediaController? = null
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
    }
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { refresh() }

    fun start() {
        try { manager.addOnActiveSessionsChangedListener(sessionsListener, listener) } catch (_: SecurityException) { return }
        refresh()
    }

    fun stop() {
        try { manager.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) {}
        watched?.unregisterCallback(callback); watched = null
        MediaStore.publish(null)
    }

    private fun refresh() {
        val controllers = try { manager.getActiveSessions(listener) } catch (_: SecurityException) { emptyList() }
        val index = pickSession(controllers.map { SessionSummary(it.packageName, it.playbackState?.state == PlaybackState.STATE_PLAYING) })
        val c = index?.let { controllers[it] }
        if (c?.sessionToken != watched?.sessionToken) {
            watched?.unregisterCallback(callback); c?.registerCallback(callback); watched = c
        }
        if (c == null) { MediaStore.publish(null); return }
        val meta = c.metadata
        MediaStore.publish(MediaInfo(
            c.packageName,
            meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
            c.playbackState?.state == PlaybackState.STATE_PLAYING,
            object : MediaControls {
                override fun previous() = c.transportControls.skipToPrevious()
                override fun togglePlay() = if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
                override fun next() = c.transportControls.skipToNext()
            },
        ))
    }
}
