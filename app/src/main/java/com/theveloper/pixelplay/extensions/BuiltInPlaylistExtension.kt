package com.theveloper.pixelplay.extensions

import org.json.JSONArray
import org.json.JSONObject

class BuiltInPlaylistExtension : PixelPlayExtension {
    override val metadata = ExtensionMetadata(
        id = "pixelplayer.playlists", name = "PixelPlayer Playlists", version = "1.0.0",
        description = "JSON playlist import and export",
        capabilities = setOf(ExtensionCapability.PLAYLIST_IMPORT, ExtensionCapability.PLAYLIST_EXPORT)
    )
    override val playlistImporter: PlaylistImporter = JsonPlaylistImporter()
    override val playlistExporter: PlaylistExporter = JsonPlaylistExporter()
}

private class JsonPlaylistExporter : PlaylistExporter {
    override val formatId = "pixelplayer-json"
    override val fileExtension = "json"
    override fun export(playlist: ProviderPlaylist): String {
        val root = JSONObject().put("format", "pixelplayer-playlist").put("version", 1)
            .put("sourceId", playlist.sourceId).put("id", playlist.id).put("title", playlist.title)
            .put("description", playlist.description).put("artworkUrl", playlist.artworkUrl)
        val tracks = JSONArray()
        playlist.tracks.forEach { t ->
            tracks.put(JSONObject().put("sourceId", t.sourceId).put("id", t.id).put("title", t.title)
                .put("artists", JSONArray(t.artists)).put("album", t.album).put("durationMs", t.durationMs)
                .put("artworkUrl", t.artworkUrl).put("mediaUri", t.mediaUri).put("explicit", t.explicit)
                .put("extra", JSONObject(t.extra)))
        }
        return root.put("tracks", tracks).toString(2)
    }
}

private class JsonPlaylistImporter : PlaylistImporter {
    override suspend fun canImport(input: String): Boolean = runCatching {
        JSONObject(input).optString("format") == "pixelplayer-playlist"
    }.getOrDefault(false)

    override suspend fun import(input: String): PlaylistImportResult {
        val root = JSONObject(input)
        require(root.optString("format") == "pixelplayer-playlist") { "Unsupported PixelPlayer playlist format" }
        val array = root.optJSONArray("tracks") ?: JSONArray()
        val tracks = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val artistsJson = item.optJSONArray("artists") ?: JSONArray()
                val artists = buildList { for (j in 0 until artistsJson.length()) add(artistsJson.optString(j)) }
                val extraJson = item.optJSONObject("extra")
                val extra = buildMap<String, String> {
                    if (extraJson != null) for (key in extraJson.keys()) put(key, extraJson.optString(key))
                }
                add(ProviderTrack(
                    sourceId = item.optString("sourceId"), id = item.optString("id"), title = item.optString("title"),
                    artists = artists, album = item.optNullableString("album"), durationMs = item.optLongOrNull("durationMs"),
                    artworkUrl = item.optNullableString("artworkUrl"), mediaUri = item.optNullableString("mediaUri"),
                    explicit = item.optBoolean("explicit"), extra = extra
                ))
            }
        }
        return PlaylistImportResult(ProviderPlaylist(
            sourceId = root.optString("sourceId"), id = root.optString("id"),
            title = root.optString("title", "Imported Playlist"),
            description = root.optNullableString("description"), artworkUrl = root.optNullableString("artworkUrl"),
            tracks = tracks
        ))
    }
}

private fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name).takeIf { it.isNotBlank() } else null
private fun JSONObject.optLongOrNull(name: String): Long? = if (has(name) && !isNull(name)) optLong(name) else null
