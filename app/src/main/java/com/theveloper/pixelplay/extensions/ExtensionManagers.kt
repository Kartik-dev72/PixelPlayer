package com.theveloper.pixelplay.extensions

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicProviderManager @Inject constructor(private val registry: ExtensionRegistry) {
    suspend fun search(query: String, limitPerProvider: Int = 30): SearchResult {
        if (query.isBlank()) return SearchResult()
        val results = registry.musicProviders().mapNotNull { runCatching { it.search(query, limitPerProvider) }.getOrNull() }
        return SearchResult(
            tracks = results.flatMap { it.tracks }, artists = results.flatMap { it.artists },
            albums = results.flatMap { it.albums }, playlists = results.flatMap { it.playlists }
        )
    }
    suspend fun resolveStream(track: ProviderTrack): StreamResult? =
        registry.get(track.sourceId)?.music?.let { runCatching { it.resolveStream(track) }.getOrNull() }
    suspend fun getTrack(track: ProviderTrack): ProviderTrack? =
        registry.get(track.sourceId)?.music?.let { runCatching { it.getTrack(track.id) }.getOrNull() }
}

@Singleton
class LyricsProviderManager @Inject constructor(private val registry: ExtensionRegistry) {
    suspend fun getLyrics(track: ProviderTrack): LyricsResult? {
        for (provider in registry.lyricsProviders()) runCatching { provider.getLyrics(track) }.getOrNull()?.let { return it }
        return null
    }
}

@Singleton
class DiscoveryManager @Inject constructor(private val registry: ExtensionRegistry) {
    suspend fun discover(): List<DiscoverySection> = registry.discoveryProviders().flatMap {
        runCatching { it.discover() }.getOrDefault(emptyList())
    }
    suspend fun similarTracks(track: ProviderTrack, limit: Int = 25): List<ProviderTrack> {
        for (provider in registry.discoveryProviders()) {
            val result = runCatching { provider.similarTracks(track, limit) }.getOrDefault(emptyList())
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }
}

@Singleton
class PlaylistManager @Inject constructor(private val registry: ExtensionRegistry) {
    suspend fun import(input: String): PlaylistImportResult? {
        for (importer in registry.playlistImporters()) {
            if (!runCatching { importer.canImport(input) }.getOrDefault(false)) continue
            runCatching { importer.import(input) }.getOrNull()?.let { return it }
        }
        return null
    }
    fun export(playlist: ProviderPlaylist, formatId: String): String? = registry.playlistExporters()
        .firstOrNull { it.formatId.equals(formatId, true) }
        ?.let { runCatching { it.export(playlist) }.getOrNull() }
}

@Singleton
class QueueBuilderManager @Inject constructor(private val registry: ExtensionRegistry) {
    suspend fun build(request: QueueRequest, builderId: String? = null): List<ProviderTrack> {
        val builders = registry.queueBuilders()
        if (builderId != null) {
            val builder = builders.firstOrNull { it.id == builderId } ?: return emptyList()
            return runCatching { builder.build(request) }.getOrDefault(emptyList())
        }
        for (builder in builders) {
            val result = runCatching { builder.build(request) }.getOrDefault(emptyList())
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }
}
