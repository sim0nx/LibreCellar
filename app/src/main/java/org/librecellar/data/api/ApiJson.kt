package org.librecellar.data.api

import kotlinx.serialization.json.Json

/**
 * JSON settings for the backend API (kotlinx.serialization).
 *
 * They keep the wire format the backend expects; JsonSerializationTest pins each of them down.
 */
val ApiJson = Json {
    // The backend may add fields (e.g. owner/access); older app versions must keep working
    ignoreUnknownKeys = true
    // Leave out null values instead of sending "field": null, which the backend rejects
    // for fields that are not nullable on its side (e.g. size)
    explicitNulls = false
    // Send fields that still have their Kotlin default (e.g. wine_type, quantity)
    encodeDefaults = true
    // A null from the server for a non-nullable field falls back to the Kotlin default
    coerceInputValues = true
}
