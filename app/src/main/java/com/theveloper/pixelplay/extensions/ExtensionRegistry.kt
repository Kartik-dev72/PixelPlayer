package com.theveloper.pixelplay.extensions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionRegistry @Inject constructor() {
    private val registered = LinkedHashMap<String, PixelPlayExtension>()
    private val _extensions = MutableStateFlow<List<PixelPlayExtension>>(emptyList())
    val extensions: StateFlow<List<PixelPlayExtension>> = _extensions.asStateFlow()

    @Synchronized
    fun register(extension: PixelPlayExtension): Boolean {
        val id = extension.metadata.id.trim()
        require(id.isNotEmpty()) { "Extension id cannot be blank" }
        if (registered.containsKey(id)) return false
        registered[id] = extension
        publish()
        return true
    }

    @Synchronized
    fun registerAll(extensions: Iterable<PixelPlayExtension>) {
        extensions.forEach { extension ->
            val id = extension.metadata.id.trim()
            if (id.isNotEmpty() && !registered.containsKey(id)) registered[id] = extension
        }
        publish()
    }

    @Synchronized fun unregister(id: String): Boolean {
        val removed = registered.remove(id) != null
        if (removed) publish()
        return removed
    }

    fun get(id: String): PixelPlayExtension? = registered[id]
    fun musicProviders() = _extensions.value.mapNotNull { it.music }
    fun lyricsProviders() = _extensions.value.mapNotNull { it.lyrics }
    fun discoveryProviders() = _extensions.value.mapNotNull { it.discovery }
    fun playlistImporters() = _extensions.value.mapNotNull { it.playlistImporter }
    fun playlistExporters() = _extensions.value.mapNotNull { it.playlistExporter }
    fun queueBuilders() = _extensions.value.mapNotNull { it.queueBuilder }
    private fun publish() { _extensions.value = registered.values.toList() }
}
