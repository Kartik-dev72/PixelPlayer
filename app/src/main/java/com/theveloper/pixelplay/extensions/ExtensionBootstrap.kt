package com.theveloper.pixelplay.extensions

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionBootstrap @Inject constructor(private val registry: ExtensionRegistry) {
    fun initialize() { registry.register(BuiltInPlaylistExtension()) }
}
