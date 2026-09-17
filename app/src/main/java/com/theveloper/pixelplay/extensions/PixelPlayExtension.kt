package com.theveloper.pixelplay.extensions

interface PixelPlayExtension {
    val metadata: ExtensionMetadata
    val music: MusicService? get() = null
    val lyrics: LyricsProvider? get() = null
    val discovery: DiscoveryProvider? get() = null
    val playlistImporter: PlaylistImporter? get() = null
    val playlistExporter: PlaylistExporter? get() = null
    val queueBuilder: QueueBuilder? get() = null
}

data class ExtensionMetadata(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: String = "",
    val website: String? = null,
    val capabilities: Set<ExtensionCapability> = emptySet()
)

enum class ExtensionCapability {
    MUSIC, LYRICS, DISCOVERY, PLAYLIST_IMPORT, PLAYLIST_EXPORT, QUEUE_BUILDER
}

data class ProviderTrack(
    val sourceId: String,
    val id: String,
    val title: String,
    val artists: List<String> = emptyList(),
    val album: String? = null,
    val durationMs: Long? = null,
    val artworkUrl: String? = null,
    val mediaUri: String? = null,
    val explicit: Boolean = false,
    val extra: Map<String, String> = emptyMap()
)

data class ProviderArtist(val sourceId: String, val id: String, val name: String, val artworkUrl: String? = null)

data class ProviderAlbum(
    val sourceId: String, val id: String, val title: String,
    val artist: String? = null, val artworkUrl: String? = null,
    val tracks: List<ProviderTrack> = emptyList()
)

data class ProviderPlaylist(
    val sourceId: String, val id: String, val title: String,
    val description: String? = null, val artworkUrl: String? = null,
    val tracks: List<ProviderTrack> = emptyList()
)

data class SearchResult(
    val tracks: List<ProviderTrack> = emptyList(),
    val artists: List<ProviderArtist> = emptyList(),
    val albums: List<ProviderAlbum> = emptyList(),
    val playlists: List<ProviderPlaylist> = emptyList()
)

data class StreamResult(
    val url: String, val mimeType: String? = null,
    val expiresAtEpochMs: Long? = null,
    val headers: Map<String, String> = emptyMap()
)

data class LyricsResult(
    val plainText: String? = null,
    val syncedLines: List<SyncedLyricLine> = emptyList(),
    val sourceName: String? = null
)

data class SyncedLyricLine(val startMs: Long, val text: String)
data class DiscoverySection(val id: String, val title: String, val tracks: List<ProviderTrack>)
data class PlaylistImportResult(val playlist: ProviderPlaylist, val warnings: List<String> = emptyList())
data class QueueRequest(val seed: ProviderTrack, val targetSize: Int = 25, val context: List<ProviderTrack> = emptyList())

interface MusicService {
    suspend fun search(query: String, limit: Int = 30): SearchResult
    suspend fun getTrack(id: String): ProviderTrack?
    suspend fun getAlbum(id: String): ProviderAlbum?
    suspend fun getArtist(id: String): ProviderArtist?
    suspend fun getPlaylist(id: String): ProviderPlaylist?
    suspend fun resolveStream(track: ProviderTrack): StreamResult?
}

interface LyricsProvider { suspend fun getLyrics(track: ProviderTrack): LyricsResult? }

interface DiscoveryProvider {
    suspend fun discover(): List<DiscoverySection>
    suspend fun similarTracks(seed: ProviderTrack, limit: Int = 25): List<ProviderTrack>
    suspend fun artistTracks(artist: ProviderArtist, limit: Int = 25): List<ProviderTrack>
}

interface PlaylistImporter {
    suspend fun canImport(input: String): Boolean
    suspend fun import(input: String): PlaylistImportResult
}

interface PlaylistExporter {
    val formatId: String
    val fileExtension: String
    fun export(playlist: ProviderPlaylist): String
}

interface QueueBuilder {
    val id: String
    val name: String
    suspend fun build(request: QueueRequest): List<ProviderTrack>
}
